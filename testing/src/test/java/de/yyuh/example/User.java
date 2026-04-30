package de.yyuh.example;

import de.yyuh.celery.api.annotation.Field;
import de.yyuh.celery.api.annotation.Identifier;
import de.yyuh.celery.api.annotation.Repository;
import de.yyuh.celery.api.entity.IEntity;

/**
 * Example entity mapped to the "users" MongoDB collection.
 *
 * <p>
 * Uses Java record with annotations for automatic serialization
 * via the Celery entity codec. {@code @Ignore} fields are excluded
 * from persistence.
 * </p>
 */
@Repository("users")
public record User(
    @Identifier String id,
    @Field("name") String name,
    @Field("email") String email) implements IEntity {

  /**
   * Create a User with a random UUID.
   */
  public User(String name, String email) {
    this(java.util.UUID.randomUUID().toString(), name, email);
  }
}
