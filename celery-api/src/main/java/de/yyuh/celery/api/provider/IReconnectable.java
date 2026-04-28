package de.yyuh.celery.api.provider;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.libs.core.result.Result;
import org.jetbrains.annotations.NotNull;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Interface for providers that support automatic reconnection on connection loss
 * or timeout.
 *
 * <p>
 * Providers implementing this interface gain automatic background health
 * monitoring and retry-based reconnection. When auto-reconnect is enabled
 * via {@link #startAutoReconnect(Credentials)}, the provider periodically
 * checks its connection status and attempts to reconnect if disconnected.
 *
 * <p>
 * The reconnection monitor uses daemon threads and a JVM shutdown hook,
 * so cleanup is fully automatic. No special handling is required in
 * {@link IProvider#close()}.
 *
 * <p>
 * This interface extends {@link IProvider} so a single connect method
 * serves both purposes. Use {@link #connectWithRetry(Credentials)} for
 * a one-shot connection attempt with retry logic, or
 * {@link #startAutoReconnect(Credentials)} for continuous background
 * monitoring.
 */
public interface IReconnectable extends IProvider {

  Logger RECONNECT_LOG = System.getLogger(IReconnectable.class.getName());

  /**
   * Returns the current reconnection options. Defaults to
   * {@link ReconnectOptions#DEFAULT}.
   *
   * @return the current reconnection options
   */
  @NotNull
  default ReconnectOptions reconnectOptions() {
    return ReconnectManager.state(this).options != null
        ? ReconnectManager.state(this).options
        : ReconnectOptions.DEFAULT;
  }

  /**
   * Sets the reconnection options.
   *
   * @param options the new options
   */
  default void reconnectOptions(final @NotNull ReconnectOptions options) {
    ReconnectManager.state(this).options = options;
  }

  /**
   * Connects with automatic retry on failure, using the configured
   * {@link ReconnectOptions} for backoff and max retries.
   *
   * @param credentials the credentials for connection
   * @return a future containing the connection result
   */
  @NotNull
  default CompletableFuture<Result<Long, String>> connectWithRetry(
      final @NotNull Credentials credentials) {
    return connectWithRetry(credentials, reconnectOptions());
  }

  /**
   * Connects with automatic retry on failure, using the given options.
   *
   * @param credentials the credentials for connection
   * @param options     the retry options
   * @return a future containing the connection result
   */
  @NotNull
  default CompletableFuture<Result<Long, String>> connectWithRetry(
      final @NotNull Credentials credentials,
      final @NotNull ReconnectOptions options) {
    return CompletableFuture.supplyAsync(() -> {
      final var state = ReconnectManager.state(this);

      for (int attempt = 0; options.maxRetries() == -1 || attempt <= options.maxRetries(); attempt++) {
        if (Thread.currentThread().isInterrupted()) {
          return Result.err("Reconnection interrupted");
        }

        try {
          final var result = connect(credentials).join();

          if (result.isOk()) {
            state.failCount.set(0);
            return result;
          }

          RECONNECT_LOG.log(Level.WARNING, "Connection attempt {0} failed: {1}", attempt, result.unwrapErr());
        } catch (final Exception e) {
          RECONNECT_LOG.log(Level.WARNING, "Connection attempt {0} threw exception: {1}", attempt, e.getMessage());
        }

        if (attempt == options.maxRetries()) {
          break;
        }

        final long delay = computeBackoff(options, attempt);

        try {
          TimeUnit.MILLISECONDS.sleep(delay);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          return Result.err("Reconnection interrupted");
        }
      }

      return Result.err("Failed to connect after " + options.maxRetries() + " attempts");
    });
  }

  /**
   * Starts automatic background reconnection monitoring.
   *
   * <p>
   * Periodically checks {@link #isConnected()} and calls {@link #connect(Credentials)}
   * if the provider is disconnected. Only one monitor is active at a time;
   * calling this when already active is a no-op.
   *
   * <p>
   * The monitor runs on a daemon thread that will automatically terminate
   * when the JVM exits. No manual resource cleanup is required.
   *
   * @param credentials the credentials for reconnection
   */
  default void startAutoReconnect(final @NotNull Credentials credentials) {
    final var state = ReconnectManager.state(this);

    if (state.scheduler != null && !state.scheduler.isShutdown()) {
      return;
    }

    state.credentials = credentials;
    state.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      final var t = new Thread(r, "reconnect-" + getClass().getSimpleName());
      t.setDaemon(true);
      t.setUncaughtExceptionHandler((thread, ex) ->
          RECONNECT_LOG.log(Level.ERROR, "Uncaught exception in reconnect monitor", ex));
      return t;
    });

    final var options = reconnectOptions();
    final ScheduledFuture<?> future = state.scheduler.scheduleAtFixedRate(
        () -> attemptReconnect(state),
        options.healthCheckInterval().toMillis(),
        options.healthCheckInterval().toMillis(),
        TimeUnit.MILLISECONDS
    );
    state.healthCheckFuture = future;

    RECONNECT_LOG.log(Level.INFO, "Auto-reconnect enabled for {0}", getClass().getSimpleName());
  }

  /**
   * Stops automatic reconnection monitoring.
   */
  default void stopAutoReconnect() {
    final var state = ReconnectManager.state(this);
    state.shutdown();
    RECONNECT_LOG.log(Level.INFO, "Auto-reconnect disabled for {0}", getClass().getSimpleName());
  }

  /**
   * Returns whether auto-reconnect monitoring is currently active.
   *
   * @return true if auto-reconnect is enabled
   */
  default boolean isAutoReconnectEnabled() {
    final var state = ReconnectManager.state(this);
    return state.scheduler != null && !state.scheduler.isShutdown();
  }

  private void attemptReconnect(final @NotNull ReconnectManager.ReconnectState state) {
    if (!state.reconnecting.compareAndSet(false, true)) {
      return;
    }

    try {
      isConnected().thenAccept(connected -> {
        if (connected) {
          state.failCount.set(0);
          return;
        }

        RECONNECT_LOG.log(Level.WARNING, "Connection lost, attempting reconnection...");

        connect(state.credentials)
            .thenAccept(result -> result
                .ifOk(time -> {
                  state.failCount.set(0);
                  RECONNECT_LOG.log(Level.INFO, "Reconnected successfully in {0}ms", time);
                })
                .ifErr(error -> {
                  state.failCount.incrementAndGet();
                  RECONNECT_LOG.log(Level.ERROR, "Reconnection failed: {0}", error);
                }))
            .exceptionally(ex -> {
              state.failCount.incrementAndGet();
              RECONNECT_LOG.log(Level.ERROR, "Reconnection threw exception", ex);
              return null;
            });
      }).exceptionally(ex -> {
        state.failCount.incrementAndGet();
        RECONNECT_LOG.log(Level.ERROR, "Health check failed", ex);
        return null;
      });
    } finally {
      state.reconnecting.set(false);
    }
  }

  private static long computeBackoff(final @NotNull ReconnectOptions options, final int attempt) {
    final long delay = (long) (options.initialDelay().toMillis()
        * Math.pow(options.backoffMultiplier(), attempt));
    return Math.min(delay, options.maxDelay().toMillis());
  }
}
