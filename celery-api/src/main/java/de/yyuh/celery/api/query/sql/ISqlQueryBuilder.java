package de.yyuh.celery.api.query.sql;

import de.yyuh.celery.api.query.IQuery;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Builds SQL query strings from query objects.
 *
 * <p>This interface provides methods to construct SELECT, INSERT, UPDATE,
 * and DELETE statements for various database operations.
 */
public interface ISqlQueryBuilder {

  /**
   * Builds a SELECT query from a query object.
   *
   * @param query the query object containing selection criteria
   * @return the generated SELECT SQL statement
   */
  @NotNull
  String buildSelect(@NotNull IQuery<?> query);

  /**
   * Builds an INSERT statement for an entity.
   *
   * @param entity the entity to insert
   * @return the generated INSERT SQL statement
   */
  @NotNull
  String buildInsert(@NotNull Object entity);

  /**
   * Builds an UPDATE statement for an entity.
   *
   * @param entity the entity to update
   * @param query the query specifying which entities to update
   * @return the generated UPDATE SQL statement
   */
  @NotNull
  String buildUpdate(@NotNull Object entity, @NotNull IQuery<?> query);

  /**
   * Builds a DELETE statement from a query object.
   *
   * @param query the query specifying which entities to delete
   * @return the generated DELETE SQL statement
   */
  @NotNull
  String buildDelete(@NotNull IQuery<?> query);

  /**
   * Returns the query parameters as a list for prepared statement binding.
   *
   * @param query the query to extract parameters from
   * @return a list of parameter values
   */
  @NotNull
  List<Object> getParameters(@NotNull IQuery<?> query);
}
