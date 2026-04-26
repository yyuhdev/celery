package de.yyuh.celery.api.query.sql;

import de.yyuh.celery.api.annotation.Repository;
import de.yyuh.celery.api.schema.SchemaExtractor;
import de.yyuh.celery.api.query.IQuery;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of ISqlQueryBuilder for relational databases.
 *
 * <p>
 * This builder generates SQL statements following standard SQL
 * conventions, including SELECT, INSERT, UPDATE, and DELETE operations.
 */
public final class DefaultSqlQueryBuilder implements ISqlQueryBuilder {

  @Override
  public @NotNull String buildSelect(final @NotNull IQuery<?> query) {
    final StringBuilder sb = new StringBuilder("SELECT * FROM ");
    sb.append(getTableName(query.entityClass()));

    appendWhereClause(sb, query);

    query.limit().ifPresent(limit -> sb.append(" LIMIT ").append(limit));
    query.offset().ifPresent(offset -> sb.append(" OFFSET ").append(offset));

    return sb.toString();
  }

  @Override
  public @NotNull String buildInsert(final @NotNull Object entity) {
    final String tableName = SchemaExtractor.getName(entity.getClass());
    final Map<String, java.lang.reflect.Field> fields = SchemaExtractor.getFields(entity.getClass());

    final String columns = String.join(", ", fields.keySet());
    final String placeholders = fields.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));

    return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
  }

  @Override
  public @NotNull String buildUpdate(final @NotNull Object entity, final @NotNull IQuery<?> query) {
    final StringBuilder sb = new StringBuilder("UPDATE ");
    sb.append(getTableName(query.entityClass()));
    sb.append(" SET ... ");

    appendWhereClause(sb, query);
    return sb.toString();
  }

  @Override
  public @NotNull String buildDelete(final @NotNull IQuery<?> query) {
    final StringBuilder sb = new StringBuilder("DELETE FROM ");
    sb.append(getTableName(query.entityClass()));

    appendWhereClause(sb, query);
    return sb.toString();
  }

  @Override
  public @NotNull List<Object> getParameters(final @NotNull IQuery<?> query) {
    return new ArrayList<>(query.filters().values());
  }

  /**
   * Appends a WHERE clause to the SQL query based on query filters.
   *
   * @param sb    the StringBuilder to append to
   * @param query the query containing filter conditions
   */
  private void appendWhereClause(final StringBuilder sb, final IQuery<?> query) {
    Map<String, Object> filters = query.filters();
    if (!filters.isEmpty()) {
      sb.append(" WHERE ");
      String conditions = filters.keySet().stream()
          .map(key -> key + " = ?")
          .collect(Collectors.joining(" AND "));
      sb.append(conditions);
    }
  }

  /**
   * Returns the table name for an entity class.
   *
   * <p>
   * If the class is annotated with @Repository, the annotation value
   * is used. Otherwise, the simple class name is used in lowercase.
   *
   * @param entityClass the entity class
   * @return the table name
   */
  @NotNull
  private String getTableName(final Class<?> entityClass) {
    if (entityClass.isAnnotationPresent(Repository.class)) {
      return entityClass.getAnnotation(Repository.class).value();
    }
    return entityClass.getSimpleName().toLowerCase();
  }
}
