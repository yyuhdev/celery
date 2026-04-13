package de.yyuh.celery.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field or record component to be excluded from database operations.
 *
 * <p>Fields annotated with @Ignore are not persisted to the database
 * and are not included in query generation.
 *
 * @see Field
 * @see Identifier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT })
public @interface Ignore {
}
