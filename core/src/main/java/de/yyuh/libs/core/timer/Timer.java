package de.yyuh.libs.core.timer;

import org.jetbrains.annotations.NotNull;
import java.util.concurrent.TimeUnit;

/**
 * Measures elapsed time between start and end points.
 *
 * <p>
 * Use this class to benchmark operations by starting a timer
 * at the beginning of an operation and calling end() to get
 * the elapsed time in milliseconds.
 */
public final class Timer {

  private final long startTime;

  private Timer(final long startTime) {
    this.startTime = startTime;
  }

  /**
   * Starts a new timer at the current time.
   *
   * @return a new Timer instance
   */
  @NotNull
  public static Timer start() {
    return new Timer(System.currentTimeMillis());
  }

  /**
   * Returns the elapsed time in milliseconds.
   *
   * @return the elapsed time in milliseconds
   */
  public long end() {
    return System.currentTimeMillis() - this.startTime;
  }

  /**
   * Returns the elapsed time in seconds.
   *
   * @return the elapsed time in seconds
   */
  public double toSeconds() {
    return this.end() / 1000.0;
  }

  /**
   * Converts the elapsed time to the specified TimeUnit.
   *
   * @param timeUnit the target time unit
   * @return the elapsed time in the specified unit
   */
  public long toTimeUnit(final @NotNull TimeUnit timeUnit) {
    return timeUnit.convert(this.end(), TimeUnit.MILLISECONDS);
  }
}
