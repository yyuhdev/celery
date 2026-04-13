package de.yyuh.celery.api.schema;

import de.yyuh.celery.api.annotation.Field;
import de.yyuh.celery.api.annotation.Repository;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extracts schema information from entity classes for database operations.
 *
 * <p>SchemaExtractor provides utilities to introspect entity classes and
 * generate database-specific schema information such as table names,
 * column mappings, and CREATE TABLE statements.
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
   */
  @NotNull
  public static Map<String, java.lang.reflect.Field> getFields(final @NotNull Class<?> entityClass) {
    final Map<String, java.lang.reflect.Field> fields = new HashMap<>();

    for (java.lang.reflect.Field field : entityClass.getDeclaredFields()) {

      String name = field.getName();
      if (field.isAnnotationPresent(Field.class)) {
        name = field.getAnnotation(Field.class).value();
      }

      fields.put(name, field);
    }
    return fields;
  }

  /**
   * Generates a CREATE TABLE SQL statement for an entity class.
   *
   * @param entityClass the entity class to generate the statement for
   * @return the CREATE TABLE SQL statement
   */
  @NotNull
  public static String generateCreateTableSql(final @NotNull Class<?> entityClass) {
    final String tableName = getName(entityClass);
    final String columns = Arrays.stream(entityClass.getDeclaredFields())
        .map(field -> {
          String name = field.isAnnotationPresent(Field.class)
              ? field.getAnnotation(Field.class).value()
              : field.getName();
          String type = getSqlType(field.getType());
          return name + " " + type;
        })
        .collect(Collectors.joining(", "));

    return "CREATE TABLE IF NOT EXISTS " + tableName + " (" + columns + ");";
  }

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
    return "TEXT";
  }
}
