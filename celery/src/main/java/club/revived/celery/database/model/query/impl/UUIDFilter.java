package club.revived.celery.database.model.query.impl;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import club.revived.celery.database.model.query.QueryFilter;

public final class UUIDFilter {

  private UUIDFilter() {
  }

  @NotNull
  public static <T> QueryFilter<T> of(
      final @NotNull Class<T> type,
      final @NotNull UUID uuid) {
    return new QueryFilter<>(
        type,
        List.of(new QueryFilter.Condition(
            "_id",
            QueryFilter.Operator.EQ,
            uuid)),
        null,
        1,
        null);
  }
}
