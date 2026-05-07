package de.yyuh.celery.api.schema;

import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import de.yyuh.celery.api.annotation.Field;
import de.yyuh.celery.api.annotation.Identifier;
import de.yyuh.celery.api.annotation.Ignore;
import de.yyuh.celery.api.annotation.Repository;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.libs.core.result.Result;

/**
 * Extracts schema information from entity classes for database operations.
 *
 * <p>
 * SchemaExtractor provides utilities to introspect entity classes and
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
   * <p>
   * If the class is annotated with @Repository, the annotation value
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
   * Extracts the identifier value from an entity.
   *
   * <p>Scans the entity's fields or record components for the field
   * annotated with {@link Identifier} and returns its value.
   * Supports both Java records and regular POJOs.
   *
   * @param entity the entity to extract the identifier from
   * @return the identifier value
   * @throws IllegalStateException if no identifier field is found or
   *                               the identifier value is null
   */
  @NotNull
  public static Object extractId(final @NotNull IEntity entity) {
    if (entity.getClass().isRecord()) {
      final var result = extractIdFromRecord(entity);

      if (result.isOk()) {
        return result.ok().get();
      }
    }

    final var result = extractIdFromPOJO(entity);

    if (result.isOk()) {
      return result.ok().get();
    }

    throw new IllegalStateException("Unable to process entity");
  }

  @NotNull
  private static Result<Object, String> extractIdFromPOJO(final IEntity entity) {
    return Result.of(() -> {
      final var components = entity.getClass().getDeclaredFields();

      final var valueOpt = Arrays.stream(components)
          .filter(component -> component.isAnnotationPresent(Identifier.class))
          .map(component -> {
            component.setAccessible(true);
            final var extractedFieldResult = extractValueFromField(component, entity);
            final var objectOpt = extractedFieldResult.ok();

            if (objectOpt.isEmpty()) {
              throw new IllegalArgumentException(extractedFieldResult.err().get());
            }

            return objectOpt.get();
          })
          .findFirst();

      return valueOpt.get();
    }).mapErr(Exception::getMessage);
  }

  @NotNull
  private static Result<Object, String> extractIdFromRecord(final IEntity entity) {
    return Result.of(() -> {
      final var components = entity.getClass().getRecordComponents();

      final var valueOpt = Arrays.stream(components)
          .filter(component -> component.isAnnotationPresent(Identifier.class))
          .map(component -> {
            final var invokedAccessor = invokeAccessor(component, entity);
            final var objectOpt = invokedAccessor.ok();

            if (objectOpt.isEmpty()) {
              throw new IllegalArgumentException(invokedAccessor.err().get());
            }

            return objectOpt.get();
          })
          .findFirst();

      return valueOpt.get();
    }).mapErr(Exception::getMessage);
  }

  @NotNull
  private static Result<Object, String> extractValueFromField(
      final java.lang.reflect.Field component,
      final IEntity entity) {
    return Result.of(() -> {
      component.setAccessible(true);

      final Object value = component.get(entity);

      if (value == null) {
        throw new IllegalArgumentException(
            "Identifier value for " + entity.getClass().getName() + " is null");
      }

      return value;

    }).mapErr(Exception::getMessage);
  }

  @NotNull
  private static Result<Object, String> invokeAccessor(final RecordComponent component, final IEntity entity) {
    return Result.of(() -> {
      final Object value = component.getAccessor().invoke(entity);

      if (value == null) {
        throw new IllegalArgumentException(
            "Identifier value for " + entity.getClass().getName() + " is null");
      }

      return value;

    }).mapErr(Exception::getMessage);
  }

  /**
   * Returns all fields of an entity class mapped to their database names.
   *
   * <p>
   * Fields annotated with @Field use the annotation value as the database
   * name. Unannotated fields use their Java field name.
   *
   * @param entityClass the entity class to extract fields from
   * @return a map of database field names to their corresponding Field objects
   * @deprecated Use {@link #getColumnNames(Class)} instead. This method relies
   *             on {@code getDeclaredFields()} which cannot resolve annotations
   *             from record components.
   */
  @NotNull
  @Deprecated(forRemoval = true)
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
}
