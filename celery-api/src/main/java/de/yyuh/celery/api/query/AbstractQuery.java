package de.yyuh.celery.api.query;

import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base implementation of IQuery providing common query functionality.
 *
 * <p>This class implements the query builder pattern and stores query
 * parameters such as filters, limit, and offset.
 *
 * @param <T> the entity type being queried
 */
public abstract class AbstractQuery<T> implements IQuery<T> {

  private final Class<T> entityClass;
  private final Map<String, Object> filters;
  private final Integer limit;
  private final Integer offset;

  /**
   * Constructs a query from a builder.
   *
   * @param builder the builder containing query parameters
   */
  protected AbstractQuery(Builder<T, ?> builder) {
    this.entityClass = builder.entityClass;
    this.filters = Collections.unmodifiableMap(new HashMap<>(builder.filters));
    this.limit = builder.limit;
    this.offset = builder.offset;
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull Class<T> entityClass() {
    return entityClass;
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull Map<String, Object> filters() {
    return filters;
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull Optional<Integer> limit() {
    return Optional.ofNullable(limit);
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull Optional<Integer> offset() {
    return Optional.ofNullable(offset);
  }

  /**
   * Abstract builder for creating query instances.
   *
   * @param <T> the entity type
   * @param <B> the builder type for method chaining
   */
  @SuppressWarnings("unchecked")
  public abstract static class Builder<T, B extends Builder<T, B>> implements IQuery.Builder<T, B> {
    protected final Class<T> entityClass;
    protected final Map<String, Object> filters = new HashMap<>();
    protected Integer limit;
    protected Integer offset;

    /**
     * Creates a new builder for the given entity class.
     *
     * @param entityClass the class of the entity to query
     */
    protected Builder(@NotNull Class<T> entityClass) {
      this.entityClass = entityClass;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull B filter(@NotNull String key, @NotNull Object value) {
      this.filters.put(key, value);
      return (B) this;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull B limit(int limit) {
      this.limit = limit;
      return (B) this;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull B offset(int offset) {
      this.offset = offset;
      return (B) this;
    }
  }
}
