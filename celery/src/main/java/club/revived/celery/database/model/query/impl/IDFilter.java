package club.revived.celery.database.model.query.impl;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import club.revived.celery.database.model.query.QueryFilter;

public final class IDFilter {

  private IDFilter() {
  }

  @NotNull
  public static <T, I> QueryFilter<T> of(
      final @NotNull Class<T> type,
      final @NotNull I id) {
    return new QueryFilter<>(
        type,
        List.of(new QueryFilter.Condition(
            "_id",
            QueryFilter.Operator.EQ,
            id)),
        null,
        1,
        null);
  }
}
