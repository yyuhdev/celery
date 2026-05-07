package de.yyuh.celery.api.event;

import com.google.protobuf.Message;

/**
 * Functional interface for handling Protobuf events.
 *
 * <p>Implementations are registered with an {@link EventBus} and invoked
 * asynchronously when a matching Protobuf message is published.
 *
 * @param <T> the Protobuf {@link Message} type to handle
 * @see EventBus
 */
@FunctionalInterface
public interface IEventHandler<T> {

  /**
   * Handles a Protobuf {@link Message} event.
   *
   * @param event the published Protobuf message
   */
  void handle(final T event);

}
