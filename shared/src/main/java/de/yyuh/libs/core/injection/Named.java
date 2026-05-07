package de.yyuh.libs.core.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifier annotation for disambiguating dependency injection.
 *
 * <p>When combined with {@link Inject}, specifies a named binding to use
 * instead of the type-based default. The {@link Injector} resolves
 * named bindings registered via {@link Injector#bindNamed(Class, String, Object)}.
 *
 * @see Inject
 * @see Injector
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Named {

  /**
   * Returns the qualifier name.
   *
   * @return the name of the binding
   */
  String value();

}
