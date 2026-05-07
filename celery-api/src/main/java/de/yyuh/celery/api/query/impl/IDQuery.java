package de.yyuh.celery.api.query.impl;

import de.yyuh.celery.api.query.AbstractQuery;
import org.jetbrains.annotations.NotNull;

/**
 * Query implementation that filters by entity ID.
 *
 * <p>Use this query type when you need to find a single entity
 * by its primary key identifier.
 *
 * @param <T> the entity type
 */
public class IDQuery<T> extends AbstractQuery<T> {

  protected IDQuery(Builder<T> builder) {
    super(builder);
  }

  /**
   * Creates a new builder for an ID-based query.
   *
   * @param entityClass the class of the entity to query
   * @param id the ID value to filter by
   * @param <T> the entity type
   * @return a new builder instance
   */
  public static <T> Builder<T> builder(@NotNull Class<T> entityClass, @NotNull Object id) {
    return new Builder<>(entityClass, id);
  }

  /**
   * Builder for creating IDQuery instances.
   *
   * @param <T> the entity type
   */
  public static class Builder<T> extends AbstractQuery.Builder<T, Builder<T>> {
    /**
     * Creates a new builder with entity class and ID filter.
     *
     * @param entityClass the class of the entity to query
     * @param id the ID value to filter by
     */
    protected Builder(@NotNull Class<T> entityClass, @NotNull Object id) {
      super(entityClass);
      filter("id", id);
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull IDQuery<T> build() {
      return new IDQuery<>(this);
    }
  }
}
