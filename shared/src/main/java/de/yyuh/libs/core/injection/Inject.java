package de.yyuh.libs.core.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field for dependency injection via {@link Injector}.
 *
 * <p>Fields annotated with {@code @Inject} are automatically populated
 * by the {@link Injector} during injection if a matching binding exists
 * for the field's type. Use {@code @Named} for qualifier-based injection.
 *
 * @see Injector
 * @see Named
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Inject {

}
