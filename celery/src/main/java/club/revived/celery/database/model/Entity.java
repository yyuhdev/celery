package club.revived.celery.database.model;

import club.revived.celery.database.Compositio;

public interface Entity {

  @SuppressWarnings("unchecked")
  default void write() {
    final var clazz = (Class<Entity>) this.getClass();
    Compositio.instance().write(clazz, this);
  }
}
