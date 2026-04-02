package club.revived.celery.messaging.exception;

import org.jetbrains.annotations.NotNull;

public final class AcknowledgmentTimeoutException extends RuntimeException {

  public AcknowledgmentTimeoutException(final @NotNull String message) {
    super(message);
  }
}
