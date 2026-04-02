package club.revived.celery.database.model.query;

import java.time.Instant;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record QueryFilter<T>(
    @NotNull Class<T> type,
    @NotNull List<Condition> conditions,
    @Nullable TimeRange time,
    @Nullable Integer limit,
    @Nullable Sort sort) {

  public record Condition(
      @NotNull String field,
      @NotNull Operator operator,
      @Nullable Object value) {
  }

  public enum Operator {
    EQ,
    NE,
    GT,
    GTE,
    LT,
    LTE,
    IN,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    EXISTS,
    NOT_EXISTS
  }

  public record TimeRange(
      @NotNull Instant from,
      @NotNull Instant to) {
  }

  public record Sort(
      @NotNull String field,
      @NotNull Direction direction) {
  }

  public enum Direction {
    ASCENDING,
    DESCENDING
  }
}
