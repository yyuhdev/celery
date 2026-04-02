package club.revived.celery.database.model.query.impl;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import club.revived.celery.database.model.query.QueryFilter;

public final class ListFilter {

  private ListFilter() {
  }

  @NotNull
  public static <T> QueryFilter<T> of(
      final @NotNull Class<T> type,
      final @NotNull String field,
      final @NotNull List<?> values) {
    return new QueryFilter<>(
        type,
        List.of(new QueryFilter.Condition(
            field,
            QueryFilter.Operator.IN,
            values)),
        null,
        null,
        null);
  }
}
