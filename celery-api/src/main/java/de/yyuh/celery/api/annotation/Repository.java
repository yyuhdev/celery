package de.yyuh.celery.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the repository/table name for an entity class.
 *
 * <p>When an entity class is annotated with @Repository, the annotation
 * value is used as the table or collection name in the database
 * instead of the class's simple name.
 *
 * @see Field
 * @see Identifier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Repository {

  /**
   * Returns the repository/table name.
   *
   * @return the table name
   */
  String value();
}
