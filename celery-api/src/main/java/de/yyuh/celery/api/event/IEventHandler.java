package de.yyuh.celery.api.event;

import com.google.protobuf.Message;

@FunctionalInterface
public interface IEventHandler<T> {

  /**
   * Handles a Protobuf {@link Message}
   */
  void handle(final T event);

}
