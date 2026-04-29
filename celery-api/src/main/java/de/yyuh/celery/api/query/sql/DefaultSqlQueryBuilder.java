package de.yyuh.celery.api.query.sql;

import de.yyuh.celery.api.annotation.Field;
import de.yyuh.celery.api.annotation.Identifier;
import de.yyuh.celery.api.annotation.Ignore;
import de.yyuh.celery.api.annotation.Repository;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.celery.api.schema.SchemaExtractor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of ISqlQueryBuilder for relational databases.
 *
 * <p>
 * This builder generates parameterized SQL statements following standard SQL
 * conventions, including SELECT, INSERT, UPDATE, and DELETE operations.
 * Statements use {@code ?} placeholders — use {@link #getParameters(IQuery)}
 * for filter params and {@link #getInsertParameters(Object)} /
 * {@link #getUpdateParameters(Object, IQuery)}
 * for positional binding.
 *
 * <p>
 * Both regular POJOs and Java {@code record} types are supported. Annotations
 * ({@code @Identifier}, {@code @Field}, {@code @Ignore}) are resolved from
 * declared fields on POJOs and from record components on records.
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
    final Class<?> clazz = entity.getClass();
    final String tableName = SchemaExtractor.getName(clazz);
    final List<ColumnInfo> columns = resolveColumns(clazz);

    if (columns.isEmpty()) {
      throw new IllegalArgumentException(
          "No persistable columns found for " + clazz.getName());
    }

    final String columnNames = columns.stream()
        .map(ColumnInfo::columnName)
        .collect(Collectors.joining(", "));
    final String placeholders = columns.stream()
        .map(c -> "?")
        .collect(Collectors.joining(", "));

    return "INSERT INTO " + tableName + " (" + columnNames + ") VALUES (" + placeholders + ")";
  }

  @Override
  public @NotNull String buildUpdate(final @NotNull Object entity, final @NotNull IQuery<?> query) {
    final Class<?> clazz = query.entityClass();
    final List<ColumnInfo> columns = resolveColumns(clazz);

    if (columns.isEmpty()) {
      throw new IllegalArgumentException(
          "No persistable columns found for " + clazz.getName());
    }

    final StringBuilder sb = new StringBuilder("UPDATE ");
    sb.append(getTableName(clazz));

    final String setClause = columns.stream()
        .filter(c -> !c.isIdentifier)
        .map(c -> c.columnName + " = ?")
        .collect(Collectors.joining(", "));

    if (setClause.isEmpty()) {
      throw new IllegalArgumentException(
          "No settable columns for " + clazz.getName() + " (all are @Identifier or @Ignore)");
    }

    sb.append(" SET ").append(setClause);

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
   * Returns the parameter values for an INSERT statement in column order.
   *
   * @param entity the entity to extract values from
   * @return a list of parameter values matching the {@code ?} placeholders
   */
  @NotNull
  public List<Object> getInsertParameters(final @NotNull Object entity) {
    final List<ColumnInfo> columns = resolveColumns(entity.getClass());
    return columns.stream()
        .map(c -> getColumnValue(entity, c))
        .collect(Collectors.toList());
  }

  /**
   * Returns the parameter values for an UPDATE statement: SET values first,
   * then WHERE filter values — matching the statement order.
   *
   * @param entity the entity to extract SET values from
   * @param query  the query providing WHERE filter values
   * @return a list of parameter values in statement order
   */
  @NotNull
  public List<Object> getUpdateParameters(
      final @NotNull Object entity,
      final @NotNull IQuery<?> query) {
    final List<ColumnInfo> columns = resolveColumns(query.entityClass());

    final List<Object> params = columns.stream()
        .filter(c -> !c.isIdentifier)
        .map(c -> getColumnValue(entity, c))
        .collect(Collectors.toCollection(ArrayList::new));

    params.addAll(query.filters().values());

    return params;
  }

  /**
   * Resolves the persistable columns for an entity class.
   *
   * <p>
   * For {@code record} types annotations are read from
   * {@link RecordComponent record components}. For regular classes
   * annotations are read from declared fields. Fields annotated with
   * {@code @Ignore} or with {@code static} modifiers are excluded.
   *
   * @param entityClass the entity class to introspect
   * @return ordered list of column metadata
   */
  @NotNull
  private List<ColumnInfo> resolveColumns(final @NotNull Class<?> entityClass) {
    if (entityClass.isRecord()) {
      return resolveRecordColumns(entityClass);
    }
    return resolvePojoColumns(entityClass);
  }

  @NotNull
  private List<ColumnInfo> resolveRecordColumns(final @NotNull Class<?> recordClass) {
    final List<ColumnInfo> columns = new ArrayList<>();

    for (final RecordComponent component : recordClass.getRecordComponents()) {
      if (component.isAnnotationPresent(Ignore.class)) {
        continue;
      }

      columns.add(new ColumnInfo(
          resolveColumnName(component),
          component.getName(),
          component.isAnnotationPresent(Identifier.class)));
    }

    return columns;
  }

  @NotNull
  private List<ColumnInfo> resolvePojoColumns(final @NotNull Class<?> pojoClass) {
    final List<ColumnInfo> columns = new ArrayList<>();

    for (final java.lang.reflect.Field field : pojoClass.getDeclaredFields()) {
      final int mod = field.getModifiers();
      if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
        continue;
      }
      if (field.isAnnotationPresent(Ignore.class)) {
        continue;
      }

      columns.add(new ColumnInfo(
          field.isAnnotationPresent(Field.class)
              ? field.getAnnotation(Field.class).value()
              : field.getName(),
          field.getName(),
          field.isAnnotationPresent(Identifier.class)));
    }

    return columns;
  }

  /**
   * Resolves the database column name for a record component.
   */
  @NotNull
  private static String resolveColumnName(final @NotNull RecordComponent component) {
    if (component.isAnnotationPresent(Field.class)) {
      return component.getAnnotation(Field.class).value();
    }
    return component.getName();
  }

  /**
   * Reads the value of a resolved column from an entity instance.
   * May return {@code null} for nullable fields.
   */
  private static Object getColumnValue(
      final @NotNull Object entity,
      final @NotNull ColumnInfo column) {
    final Class<?> clazz = entity.getClass();

    try {
      if (clazz.isRecord()) {
        for (final RecordComponent component : clazz.getRecordComponents()) {
          if (component.getName().equals(column.javaName)) {
            return component.getAccessor().invoke(entity);
          }
        }
        throw new IllegalArgumentException(
            "No record component '" + column.javaName + "' in " + clazz.getName());
      }

      final java.lang.reflect.Field field = clazz.getDeclaredField(column.javaName);
      field.setAccessible(true);
      return field.get(entity);
    } catch (final ReflectiveOperationException e) {
      throw new RuntimeException(
          "Failed to read field '" + column.javaName + "' from " + clazz.getName(), e);
    }
  }

  /**
   * Appends a WHERE clause to the SQL query based on query filters.
   *
   * @param sb    the StringBuilder to append to
   * @param query the query containing filter conditions
   */
  private void appendWhereClause(final StringBuilder sb, final IQuery<?> query) {
    final Map<String, Object> filters = query.filters();
    if (!filters.isEmpty()) {
      sb.append(" WHERE ");
      final String conditions = filters.keySet().stream()
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
  private static String getTableName(final Class<?> entityClass) {
    if (entityClass.isAnnotationPresent(Repository.class)) {
      return entityClass.getAnnotation(Repository.class).value();
    }
    return entityClass.getSimpleName().toLowerCase();
  }

  /**
   * Metadata for a single persistable column.
   *
   * @param columnName   the database column name (resolved from {@code @Field} or
   *                     java name)
   * @param javaName     the Java field / record component name
   * @param isIdentifier whether this column is the primary key
   */
  private record ColumnInfo(
      @NotNull String columnName,
      @NotNull String javaName,
      boolean isIdentifier) {
  }
}
