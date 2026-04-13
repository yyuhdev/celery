package de.yyuh.libs.core.event;

import com.google.protobuf.Message;

@FunctionalInterface
public interface IEventHandler<T extends Message> {

  /**
   * Handles a Protobuf {@link Message}
   */
  void handle(final T event);

}
