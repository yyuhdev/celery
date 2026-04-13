package de.yyuh.celery.api.query.impl;

import de.yyuh.celery.api.query.AbstractQuery;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

/**
 * Query implementation that filters by UUID field.
 *
 * <p>Use this query type when entities are identified by UUID
 * and you need to find a specific entity by its UUID value.
 *
 * @param <T> the entity type
 */
public class UUIDQuery<T> extends AbstractQuery<T> {

  protected UUIDQuery(final Builder<T> builder) {
    super(builder);
  }

  /**
   * Creates a new builder for a UUID-based query.
   *
   * @param entityClass the class of the entity to query
   * @param uuid the UUID value to filter by
   * @param <T> the entity type
   * @return a new builder instance
   */
  @NotNull
  public static <T> Builder<T> builder(final @NotNull Class<T> entityClass, final @NotNull UUID uuid) {
    return new Builder<>(entityClass, uuid);
  }

  /**
   * Builder for creating UUIDQuery instances.
   *
   * @param <T> the entity type
   */
  public static class Builder<T> extends AbstractQuery.Builder<T, Builder<T>> {
    /**
     * Creates a new builder with entity class and UUID filter.
     *
     * @param entityClass the class of the entity to query
     * @param uuid the UUID value to filter by
     */
    protected Builder(final @NotNull Class<T> entityClass, final @NotNull UUID uuid) {
      super(entityClass);
      filter("uuid", uuid);
    }

    @Override
    public @NotNull UUIDQuery<T> build() {
      return new UUIDQuery<>(this);
    }
  }
}
