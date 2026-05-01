package de.yyuh.celery.api.event;

import java.util.concurrent.TimeUnit;

import com.google.protobuf.Message;

public final class Subscriber {

  private final Message message;
  private final Expiry expiry;

  private int timesCalled;

  public Subscriber(
      final Message message,
      final Expiry expiry) {
    this.message = message;
    this.expiry = expiry;
  }

  public Message getMessage() {
    return message;
  }

  public Expiry getExpiry() {
    return expiry;
  }

  public int getTimesCalled() {
    return timesCalled;
  }

  public void setTimesCalled(int timesCalled) {
    this.timesCalled = timesCalled;
  }

  public class Expiry {

    private final int expireAfter;
    private final TimeUnit timeUnit;

    public Expiry() {
      this(-1);
    }

    public Expiry(final int expireAfter) {
      this(expireAfter, null);
    }

    public Expiry(
        final int expireAfter,
        final TimeUnit timeUnit) {
      this.expireAfter = expireAfter;
      this.timeUnit = timeUnit;
    }

    public int getExpireAfter() {
      return expireAfter;
    }

    public TimeUnit getTimeUnit() {
      return timeUnit;
    }

  }

}
