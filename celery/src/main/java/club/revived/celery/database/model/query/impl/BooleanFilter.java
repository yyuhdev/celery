package club.revived.celery.database.model.query.impl;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import club.revived.celery.database.model.query.QueryFilter;

public final class BooleanFilter {

  private BooleanFilter() {
  }

  @NotNull
  public static <T> QueryFilter<T> of(
      final @NotNull Class<T> type,
      final @NotNull String field,
      final boolean value) {
    return new QueryFilter<>(
        type,
        List.of(new QueryFilter.Condition(
            field,
            QueryFilter.Operator.EQ,
            value)),
        null,
        null,
        null);
  }
}
