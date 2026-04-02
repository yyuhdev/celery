package club.revived.celery.test.model;

import java.util.UUID;

import club.revived.celery.database.model.Entity;
import club.revived.celery.database.model.annotation.Identifier;
import club.revived.celery.database.model.annotation.Property;
import club.revived.celery.database.model.annotation.Repository;

/**
 * Test entity representing a user for testing purposes.
 */
@Repository("test_users")
public record TestUser(
    @Identifier UUID id,
    @Property("user_name") String username,
    String email,
    int age,
    boolean active) implements Entity {

  public static TestUser create(final String username, final String email, final int age) {
    return new TestUser(UUID.randomUUID(), username, email, age, true);
  }

  public static TestUser create(final UUID id, final String username, final String email, final int age) {
    return new TestUser(id, username, email, age, true);
  }
}
