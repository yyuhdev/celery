package de.yyuh.celery.api.schema;

import de.yyuh.celery.api.annotation.Field;
import de.yyuh.celery.api.annotation.Identifier;
import de.yyuh.celery.api.annotation.Ignore;
import de.yyuh.celery.api.annotation.Repository;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Extracts schema information from entity classes for database operations.
 *
 * <p>SchemaExtractor provides utilities to introspect entity classes and
 * generate database-specific schema information such as table names,
 * column mappings, and CREATE TABLE statements. Both Java {@code record}
 * types and regular POJOs are supported.
 */
public final class SchemaExtractor {

  private SchemaExtractor() {
  }

  /**
   * Returns the table or collection name for an entity class.
   *
   * <p>If the class is annotated with @Repository, the annotation value
   * is used. Otherwise, the simple class name is used in lowercase.
   *
   * @param entityClass the entity class to get the name for
   * @return the table or collection name
   */
  @NotNull
  public static String getName(final @NotNull Class<?> entityClass) {
    if (entityClass.isAnnotationPresent(Repository.class)) {
      return entityClass.getAnnotation(Repository.class).value();
    }
    return entityClass.getSimpleName().toLowerCase();
  }

  /**
   * Returns all fields of an entity class mapped to their database names.
   *
   * <p>Fields annotated with @Field use the annotation value as the database
   * name. Unannotated fields use their Java field name.
   *
   * @param entityClass the entity class to extract fields from
   * @return a map of database field names to their corresponding Field objects
   * @deprecated Use {@link #getColumnNames(Class)} instead. This method relies
   *             on {@code getDeclaredFields()} which cannot resolve annotations
   *             from record components.
   */
  @Deprecated
  @NotNull
  public static Map<String, java.lang.reflect.Field> getFields(final @NotNull Class<?> entityClass) {
    final Map<String, java.lang.reflect.Field> fields = new LinkedHashMap<>();

    for (java.lang.reflect.Field field : entityClass.getDeclaredFields()) {
      final int mod = field.getModifiers();
      if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
        continue;
      }
      if (field.isAnnotationPresent(Ignore.class)) {
        continue;
      }

      String name = field.getName();
      if (field.isAnnotationPresent(Field.class)) {
        name = field.getAnnotation(Field.class).value();
      }

      fields.put(name, field);
    }
    return fields;
  }

  /**
   * Returns the ordered list of database column names for an entity class.
   *
   * <p>Works for both records and POJOs. Resolves {@code @Field} names and
   * excludes {@code @Ignore}, static, and transient members.
   *
   * @param entityClass the entity class to introspect
   * @return ordered list of database column names (never {@code null})
   */
  @NotNull
  public static List<String> getColumnNames(final @NotNull Class<?> entityClass) {
    return resolveColumns(entityClass).stream()
        .map(ColumnMeta::name)
        .collect(Collectors.toList());
  }

  /**
   * Returns the database column name for the {@code @Identifier} field, if any.
   *
   * @param entityClass the entity class to introspect
   * @return the identifier column name, or {@code empty} if none is annotated
   */
  @NotNull
  public static Optional<String> getIdentifierColumnName(final @NotNull Class<?> entityClass) {
    return resolveColumns(entityClass).stream()
        .filter(ColumnMeta::isIdentifier)
        .map(ColumnMeta::name)
        .findFirst();
  }

  /**
   * Generates a CREATE TABLE SQL statement for an entity class.
   *
   * <p>Columns annotated with {@code @Identifier} get a {@code PRIMARY KEY}
   * constraint. Columns annotated with {@code @Ignore} are excluded.
   * Both records and POJOs are supported.
   *
   * @param entityClass the entity class to generate the statement for
   * @return the CREATE TABLE SQL statement
   */
  @NotNull
  public static String generateCreateTableSql(final @NotNull Class<?> entityClass) {
    final String tableName = getName(entityClass);
    final List<ColumnMeta> columns = resolveColumns(entityClass);

    final String columnDefs = columns.stream()
        .map(col -> {
          final String sqlType = getSqlType(col.type());
          final String name = col.name();
          if (col.isIdentifier()) {
            return name + " " + sqlType + " PRIMARY KEY";
          }
          return name + " " + sqlType;
        })
        .collect(Collectors.joining(", "));

    return "CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnDefs + ");";
  }

  // ──────────────────────────────────────────────────────────────
  // Column resolution
  // ──────────────────────────────────────────────────────────────

  /**
   * Resolves persistable columns for a class, handling both records and POJOs.
   */
  @NotNull
  private static List<ColumnMeta> resolveColumns(final @NotNull Class<?> entityClass) {
    if (entityClass.isRecord()) {
      return resolveRecordColumns(entityClass);
    }
    return resolvePojoColumns(entityClass);
  }

  @NotNull
  private static List<ColumnMeta> resolveRecordColumns(final @NotNull Class<?> recordClass) {
    final List<ColumnMeta> columns = new ArrayList<>();

    for (final RecordComponent component : recordClass.getRecordComponents()) {
      if (component.isAnnotationPresent(Ignore.class)) {
        continue;
      }

      columns.add(new ColumnMeta(
          resolveColumnName(component),
          component.getType(),
          component.isAnnotationPresent(Identifier.class)));
    }

    return columns;
  }

  @NotNull
  private static List<ColumnMeta> resolvePojoColumns(final @NotNull Class<?> pojoClass) {
    final List<ColumnMeta> columns = new ArrayList<>();

    for (final java.lang.reflect.Field field : pojoClass.getDeclaredFields()) {
      final int mod = field.getModifiers();
      if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
        continue;
      }
      if (field.isAnnotationPresent(Ignore.class)) {
        continue;
      }

      columns.add(new ColumnMeta(
          field.isAnnotationPresent(Field.class)
              ? field.getAnnotation(Field.class).value()
              : field.getName(),
          field.getType(),
          field.isAnnotationPresent(Identifier.class)));
    }

    return columns;
  }

  @NotNull
  private static String resolveColumnName(final @NotNull RecordComponent component) {
    if (component.isAnnotationPresent(Field.class)) {
      return component.getAnnotation(Field.class).value();
    }
    return component.getName();
  }

  // ──────────────────────────────────────────────────────────────
  // SQL type mapping
  // ──────────────────────────────────────────────────────────────

  /**
   * Maps a Java type to its corresponding SQL type.
   *
   * @param type the Java class type to convert
   * @return the SQL type string
   */
  @NotNull
  private static String getSqlType(final Class<?> type) {
    if (type == String.class || type == java.util.UUID.class)
      return "VARCHAR(255)";
    if (type == int.class || type == Integer.class)
      return "INT";
    if (type == long.class || type == Long.class)
      return "BIGINT";
    if (type == boolean.class || type == Boolean.class)
      return "BOOLEAN";
    if (type == double.class || type == Double.class || type == float.class || type == Float.class)
      return "DOUBLE";
    return "TEXT";
  }

  // ──────────────────────────────────────────────────────────────
  // Column metadata
  // ──────────────────────────────────────────────────────────────

  /**
   * Internal metadata for a resolved database column.
   */
  private record ColumnMeta(
      @NotNull String name,
      @NotNull Class<?> type,
      boolean isIdentifier) {
  }
}
