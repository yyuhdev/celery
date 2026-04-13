package de.yyuh.celery.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field or record component as the entity's primary identifier.
 *
 * <p>The annotated field represents the unique identifier for the entity
 * and is used for upsert and lookup operations. In MongoDB, this field
 * is stored as {@code _id}.
 *
 * @see Field
 * @see Ignore
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT })
public @interface Identifier {
}
