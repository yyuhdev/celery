package de.yyuh.libs.core.injection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import de.yyuh.libs.core.result.Result;

public final class Injector {

  private final Map<Class<?>, Object> bindings = new HashMap<>();
  private final Map<Class<?>, Map<String, Object>> namedBindings = new HashMap<>();

  public <T> void bind(
      final Class<T> type,
      final T instance) {
    bindings.put(type, instance);
  }

  public <T> void bindNamed(
      final Class<T> type,
      final String name,
      final T instance) {
    namedBindings.computeIfAbsent(type, k -> new HashMap<>()).put(name, instance);
  }

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
