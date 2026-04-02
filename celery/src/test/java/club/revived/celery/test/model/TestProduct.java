package club.revived.celery.test.model;

import java.util.UUID;

import club.revived.celery.database.model.Entity;
import club.revived.celery.database.model.annotation.Identifier;
import club.revived.celery.database.model.annotation.Property;
import club.revived.celery.database.model.annotation.Repository;

/**
 * Test entity representing a product for testing purposes.
 */
@Repository("test_products")
public record TestProduct(
    @Identifier UUID id,
    @Property("product_name") String name,
    String category,
    double price,
    int stock,
    boolean available) implements Entity {

  public static TestProduct create(final String name, final String category, final double price, final int stock) {
    return new TestProduct(UUID.randomUUID(), name, category, price, stock, stock > 0);
  }

  public static TestProduct create(final UUID id, final String name, final String category, final double price, final int stock) {
    return new TestProduct(id, name, category, price, stock, stock > 0);
  }
}
