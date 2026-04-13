package de.yyuh.celery.api.query;

import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a database query with filters, pagination, and ordering options.
 *
 * <p>Queries are immutable and should be constructed using the builder pattern.
 * They serve as a portable representation of database queries across
 * different database providers.
 *
 * @param <T> the entity type being queried
 */
public interface IQuery<T> {

  /**
   * Returns the target entity class for this query.
   *
   * @return the entity class
   */
  @NotNull
  Class<T> entityClass();

  /**
   * Returns the query filters as key-value pairs.
   *
   * @return an unmodifiable map of filter conditions
   */
  @NotNull
  Map<String, Object> filters();

  /**
   * Returns the maximum number of results to return.
   *
   * @return an Optional containing the limit, or empty if no limit
   */
  @NotNull
  Optional<Integer> limit();

  /**
   * Returns the number of results to skip.
   *
   * @return an Optional containing the offset, or empty if no offset
   */
  @NotNull
  Optional<Integer> offset();

  /**
   * Builder interface for constructing query instances.
   *
   * @param <T> the entity type
   * @param <B> the builder type for method chaining
   */
  interface Builder<T, B extends Builder<T, B>> {
    /**
     * Adds a filter condition to the query.
     *
     * @param key the field name to filter on
     * @param value the value to match
     * @return this builder for method chaining
     */
    @NotNull
    B filter(@NotNull String key, @NotNull Object value);

    /**
     * Sets the maximum number of results.
     *
     * @param limit the maximum number of results
     * @return this builder for method chaining
     */
    @NotNull
    B limit(int limit);

    /**
     * Sets the number of results to skip.
     *
     * @param offset the number of results to skip
     * @return this builder for method chaining
     */
    @NotNull
    B offset(int offset);

    /**
     * Builds the query instance.
     *
     * @return the constructed query
     */
    @NotNull
    IQuery<T> build();
  }
}
