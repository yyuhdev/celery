package de.yyuh.celery.api.provider;

import de.yyuh.celery.api.credentials.Credentials;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Internal helper that manages per-instance reconnection state for
 * {@link IReconnectable} default methods.
 */
final class ReconnectManager {

  private static final ConcurrentHashMap<IReconnectable, ReconnectState> states = new ConcurrentHashMap<>();

  private ReconnectManager() {}

  static ReconnectState state(final IReconnectable provider) {
    return states.computeIfAbsent(provider, k -> new ReconnectState());
  }

  static void removeState(final IReconnectable provider) {
    final var state = states.remove(provider);
    if (state != null) {
      state.shutdown();
    }
  }

  static final class ReconnectState {
    volatile ScheduledExecutorService scheduler;
    volatile ScheduledFuture<?> healthCheckFuture;
    volatile Credentials credentials;
    volatile ReconnectOptions options;
    final AtomicBoolean reconnecting = new AtomicBoolean(false);
    final AtomicInteger failCount = new AtomicInteger(0);

    void shutdown() {
      if (healthCheckFuture != null) {
        healthCheckFuture.cancel(false);
        healthCheckFuture = null;
      }
      if (scheduler != null && !scheduler.isShutdown()) {
        scheduler.shutdownNow();
        scheduler = null;
      }
    }
  }
}
