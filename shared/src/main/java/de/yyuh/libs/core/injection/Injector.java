package de.yyuh.libs.core.injection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import de.yyuh.libs.core.result.Result;

/**
 * Simple dependency injection container.
 *
 * <p>A lightweight, reflection-based DI container that supports type-based
 * and named bindings. Fields annotated with {@link Inject} are automatically
 * populated during {@link #inject(Object)}. Named bindings (via {@link Named})
 * enable disambiguation when multiple instances of the same type exist.
 *
 * <p>Fields are resolved by traversing the full class hierarchy, including
 * superclass fields.
 *
 * <pre>{@code
 * Injector injector = new Injector();
 * injector.bind(EventBus.class, myEventBus);
 * injector.inject(myService); // populates @Inject fields on myService
 * }</pre>
 *
 * @see Inject
 * @see Named
 */
public final class Injector {

  private final Map<Class<?>, Object> bindings = new HashMap<>();
  private final Map<Class<?>, Map<String, Object>> namedBindings = new HashMap<>();

  /**
   * Binds a type to an instance for type-based injection.
   *
   * @param type     the type to bind
   * @param instance the instance to inject for that type
   * @param <T>      the type parameter
   */
  public <T> void bind(
      final Class<T> type,
      final T instance) {
    bindings.put(type, instance);
  }

  /**
   * Binds a type to a named instance for qualifier-based injection.
   *
   * @param type     the type to bind
   * @param name     the qualifier name
   * @param instance the instance to inject
   * @param <T>      the type parameter
   */
  public <T> void bindNamed(
      final Class<T> type,
      final String name,
      final T instance) {
    namedBindings.computeIfAbsent(type, k -> new HashMap<>()).put(name, instance);
  }

  /**
   * Injects dependencies into all {@link Inject}-annotated fields of the target.
   *
   * <p>Walks the class hierarchy to discover fields declared in superclasses.
   * Fields without a matching binding are silently skipped.
   *
   * @param target the object to inject dependencies into
   */
  public void inject(final Object target) {
    for (final Field field : this.getAllFields(target.getClass())) {
      if (!field.isAnnotationPresent(Inject.class)) {
        continue;
      }

      final Object value = this.resolve(field);

      if (value == null) {
        continue;
      }

      Result.of(() -> {
        field.setAccessible(true);
        field.set(target, value);

        return null;
      });
    }
  }

  @NotNull
  private Object resolve(final Field field) {
    final Class<?> type = field.getType();
    final Named named = field.getAnnotation(Named.class);

    if (named != null) {
      final Map<String, Object> map = namedBindings.get(type);

      return map != null ? map.get(named.value()) : null;
    }

    return bindings.get(type);
  }

  @NotNull
  private List<Field> getAllFields(final Class<?> clazz) {
    final List<Field> fields = new ArrayList<>();
    Class<?> current = clazz;

    while (current != null) {
      fields.addAll(Arrays.asList(current.getDeclaredFields()));
      current = current.getSuperclass();
    }

    return fields;
  }
}
