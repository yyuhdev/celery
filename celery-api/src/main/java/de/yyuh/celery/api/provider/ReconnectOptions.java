package de.yyuh.celery.api.provider;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Configuration options for provider reconnection behavior.
 *
 * @param maxRetries        maximum number of reconnection attempts (-1 for unlimited)
 * @param initialDelay      delay before the first retry
 * @param maxDelay          maximum delay between retries
 * @param backoffMultiplier multiplier for exponential backoff
 * @param healthCheckInterval interval between health checks when auto-reconnect is enabled
 */
public record ReconnectOptions(
    int maxRetries,
    @NotNull Duration initialDelay,
    @NotNull Duration maxDelay,
    double backoffMultiplier,
    @NotNull Duration healthCheckInterval
) {

  /**
   * Default reconnection options: 10 retries, 1s initial delay, 1min max delay,
   * 2x backoff multiplier, 30s health check interval.
   */
  public static final ReconnectOptions DEFAULT = new ReconnectOptions(
      10,
      Duration.ofSeconds(1),
      Duration.ofMinutes(1),
      2.0,
      Duration.ofSeconds(30)
  );

  public ReconnectOptions {
    if (maxRetries < -1) throw new IllegalArgumentException("maxRetries must be >= -1");
    if (initialDelay.isNegative() || initialDelay.isZero())
      throw new IllegalArgumentException("initialDelay must be positive");
    if (maxDelay.compareTo(initialDelay) < 0)
      throw new IllegalArgumentException("maxDelay must be >= initialDelay");
    if (backoffMultiplier < 1.0)
      throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
    if (healthCheckInterval.isNegative() || healthCheckInterval.isZero())
      throw new IllegalArgumentException("healthCheckInterval must be positive");
  }
}
