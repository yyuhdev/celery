package de.yyuh.celery.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the database column name for a field or record component.
 *
 * <p>When a field or record component is annotated with @Field, the
 * annotation value is used as the column/field name in the database
 * instead of the Java field name.
 *
 * @see Repository
 * @see Identifier
 * @see Ignore
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT })
public @interface Field {

  /**
   * Returns the database column name.
   *
   * @return the column name
   */
  String value();
}
