package de.yyuh.celery.api.query.sql;

import de.yyuh.celery.api.query.IQuery;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ISqlQueryBuilder {

  @NotNull
  String buildSelect(@NotNull IQuery<?> query);

  @NotNull
  String buildInsert(@NotNull Object entity);

  @NotNull
  String buildUpdate(@NotNull Object entity, @NotNull IQuery<?> query);

  @NotNull
  String buildDelete(@NotNull IQuery<?> query);

  @NotNull
  List<Object> getParameters(@NotNull IQuery<?> query);
}
