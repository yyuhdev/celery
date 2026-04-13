package de.yyuh.celery.api.entity;

/**
 * Marker interface for database entities.
 *
 * <p>Entities are objects that can be persisted to and retrieved from
 * a database. Classes implementing this interface should be annotated
 * with {@code @Identifier} on the field that represents the primary key.
 */
public interface IEntity {

  /**
   * Saves this entity to the database.
   */
  @SuppressWarnings("unchecked")
  default void save() {
  }
}
