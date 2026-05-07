package de.yyuh.celery.api.event;

import java.util.concurrent.TimeUnit;

import com.google.protobuf.Message;

/**
 * Represents a subscription to an {@link EventBus}.
 *
 * <p>A subscriber binds a Protobuf message instance (as a type placeholder)
 * to an expiry policy. The expiry can be based on call count (e.g. expire
 * after 5 invocations) or a time duration (e.g. expire after 60 seconds).
 *
 * <p>Subscriptions with no expiry ({@code expireAfter = -1}) are permanent.
 *
 * @see EventBus
 * @see IEventHandler
 */
public final class Subscriber {

  private final Message message;
  private final Expiry expiry;

  private int timesCalled;

  /**
   * Creates a new subscriber.
   *
   * @param message the Protobuf message instance used as a type reference
   * @param expiry  the expiry policy; {@code null} means no expiry
   */
  public Subscriber(
      final Message message,
      final Expiry expiry) {
    this.message = message;
    this.expiry = expiry;
  }

  /**
   * Returns the Protobuf message instance used as the type reference.
   *
   * @return the message instance
   */
  public Message getMessage() {
    return message;
  }

  /**
   * Returns the expiry policy for this subscription.
   *
   * @return the expiry, or {@code null} if no expiry is set
   */
  public Expiry getExpiry() {
    return expiry;
  }

  /**
   * Returns the number of times this subscriber has been invoked.
   *
   * @return the invocation count
   */
  public int getTimesCalled() {
    return timesCalled;
  }

  /**
   * Sets the number of times this subscriber has been invoked.
   *
   * @param timesCalled the new invocation count
   */
  public void setTimesCalled(int timesCalled) {
    this.timesCalled = timesCalled;
  }

  /**
   * Expiry policy for a subscriber.
   *
   * <p>An expiry can be based on call count only (no time unit) or a
   * time-based duration. A value of {@code -1} means the subscription
   * never expires.
   */
  public class Expiry {

    private final int expireAfter;
    private final TimeUnit timeUnit;

    /**
     * Creates an expiry that never expires.
     */
    public Expiry() {
      this(-1);
    }

    /**
     * Creates a call-count-based expiry.
     *
     * @param expireAfter the number of invocations before expiry
     */
    public Expiry(final int expireAfter) {
      this(expireAfter, null);
    }

    /**
     * Creates a time-based expiry.
     *
     * @param expireAfter the duration value
     * @param timeUnit    the time unit; if {@code null} the expiry is count-based
     */
    public Expiry(
        final int expireAfter,
        final TimeUnit timeUnit) {
      this.expireAfter = expireAfter;
      this.timeUnit = timeUnit;
    }

    /**
     * Returns the number of invocations or duration before expiry.
     *
     * @return the expiry threshold, or {@code -1} for no expiry
     */
    public int getExpireAfter() {
      return expireAfter;
    }

    /**
     * Returns the time unit for this expiry.
     *
     * @return the time unit, or {@code null} if count-based
     */
    public TimeUnit getTimeUnit() {
      return timeUnit;
    }

  }

}
