package club.revived.celery.database.model.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public final class QueryFilterBuilder<T> {

  private final Class<T> type;
  private final List<QueryFilter.Condition> conditions = new ArrayList<>();
  private QueryFilter.TimeRange time;
  private Integer limit;
  private QueryFilter.Sort sort;

  public QueryFilterBuilder(final @NotNull Class<T> type) {
    this.type = type;
  }

  @NotNull
  public QueryFilterBuilder<T> where(
      final @NotNull String field,
      final @NotNull QueryFilter.Operator operator,
      final @NotNull Object value) {
    conditions.add(new QueryFilter.Condition(
        field,
        operator,
        value));
    return this;
  }

  @NotNull
  public QueryFilterBuilder<T> eq(
      final @NotNull String field,
      final @NotNull Object value) {
    return where(field, QueryFilter.Operator.EQ, value);
  }

  @NotNull
  public QueryFilterBuilder<T> gt(
      final @NotNull String field,
      final @NotNull Object value) {
    return where(field, QueryFilter.Operator.GT, value);
  }

  @NotNull
  public QueryFilterBuilder<T> gte(
      final @NotNull String field,
      final @NotNull Object value) {
    return where(field, QueryFilter.Operator.GTE, value);
  }

  @NotNull
  public QueryFilterBuilder<T> lt(
      final @NotNull String field,
      final @NotNull Object value) {
    return where(field, QueryFilter.Operator.LT, value);
  }

  @NotNull
  public QueryFilterBuilder<T> lte(
      final @NotNull String field,
      final @NotNull Object value) {
    return where(field, QueryFilter.Operator.LTE, value);
  }

  @NotNull
  public QueryFilterBuilder<T> ne(
      final @NotNull String field,
      final @NotNull Object value) {
    return where(field, QueryFilter.Operator.NE, value);
  }

  @NotNull
  public QueryFilterBuilder<T> in(
      final @NotNull String field,
      final @NotNull List<?> values) {
    return where(field, QueryFilter.Operator.IN, values);
  }
  
  @NotNull
  public QueryFilterBuilder<T> contains(
      final @NotNull String field,
      final @NotNull String value) {
    return where(field, QueryFilter.Operator.CONTAINS, value);
  }

  @NotNull
  public QueryFilterBuilder<T> startsWith(
      final @NotNull String field,
      final @NotNull String value) {
    return where(field, QueryFilter.Operator.STARTS_WITH, value);
  }

  @NotNull
  public QueryFilterBuilder<T> endsWith(
      final @NotNull String field,
      final @NotNull String value) {
    return where(field, QueryFilter.Operator.ENDS_WITH, value);
  }

  @NotNull
  public QueryFilterBuilder<T> exists(final @NotNull String field) {
    return where(field, QueryFilter.Operator.EXISTS, true);
  }

  @NotNull
  public QueryFilterBuilder<T> notExists(final @NotNull String field) {
    return where(field, QueryFilter.Operator.NOT_EXISTS, true);
  }

  @NotNull
  public QueryFilterBuilder<T> between(
      final @NotNull Instant from,
      final @NotNull Instant to) {
    this.time = new QueryFilter.TimeRange(
        from,
        to);
    return this;
  }

  @NotNull
  public QueryFilterBuilder<T> limit(final @NotNull Integer limit) {
    this.limit = limit;
    return this;
  }

  @NotNull
  public QueryFilterBuilder<T> sort(
      final @NotNull String field,
      final @NotNull QueryFilter.Direction direction) {
    this.sort = new QueryFilter.Sort(field, direction);
    return this;
  }

  @NotNull
  public QueryFilterBuilder<T> sortAsc(final @NotNull String field) {
    return sort(field, QueryFilter.Direction.ASCENDING);
  }

  @NotNull
  public QueryFilterBuilder<T> sortDesc(final @NotNull String field) {
    return sort(field, QueryFilter.Direction.DESCENDING);
  }

  @NotNull
  public QueryFilter<T> build() {
    return new QueryFilter<>(
        type,
        List.copyOf(conditions),
        time,
        limit,
        sort);
  }
}
