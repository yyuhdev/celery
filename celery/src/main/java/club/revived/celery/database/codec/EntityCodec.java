package club.revived.celery.database.codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.jetbrains.annotations.NotNull;

import club.revived.celery.database.model.Entity;
import club.revived.celery.database.model.annotation.Identifier;
import club.revived.celery.database.model.annotation.Ignore;
import club.revived.celery.database.model.annotation.Property;

public final class EntityCodec<T extends Entity> implements Codec<T> {

  private final @NotNull Class<T> clazz;
  private final @NotNull CodecRegistry registry;
  private final @NotNull Constructor<T> constructor;
  private final @NotNull RecordComponent[] components;

  public EntityCodec(
      @NotNull final Class<T> clazz,
      @NotNull final CodecRegistry registry) {

    if (!clazz.isRecord()) {
      throw new IllegalArgumentException("EntityCodec supports records only.");
    }

    this.clazz = clazz;
    this.registry = registry;
    this.components = clazz.getRecordComponents();

    try {
      final var parameterTypes = Arrays.stream(components)
          .map(RecordComponent::getType)
          .toArray(Class<?>[]::new);

      this.constructor = clazz.getDeclaredConstructor(parameterTypes);
      this.constructor.setAccessible(true);

    } catch (final NoSuchMethodException exception) {
      throw new IllegalStateException(
          "Failed to initialize codec for " + clazz.getName(),
          exception);
    }
  }

  @Override
  public void encode(
      @NotNull final BsonWriter writer,
      @NotNull final T value,
      @NotNull final EncoderContext encoderContext) {

    writer.writeStartDocument();

    try {
      for (final var component : components) {
        final var fieldName = getFieldName(component);
        final var fieldValue = component.getAccessor().invoke(value);

        writer.writeName(fieldName);

        if (fieldValue == null || component.isAnnotationPresent(Ignore.class)) {
          writer.writeNull();
          continue;
        }

        @SuppressWarnings("unchecked")
        final var codec = (Codec<Object>) registry.get(fieldValue.getClass());

        encoderContext.encodeWithChildContext(codec, writer, fieldValue);
      }

    } catch (final ReflectiveOperationException exception) {
      throw new IllegalStateException(
          "Failed to encode " + clazz.getName(),
          exception);
    }

    writer.writeEndDocument();
  }

  @Override
  public T decode(
      @NotNull final BsonReader reader,
      @NotNull final DecoderContext decoderContext) {

    final var values = new HashMap<String, Object>();

    reader.readStartDocument();

    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
      final var name = reader.readName();
      final var component = findComponent(name);

      final Object value;

      if (reader.getCurrentBsonType() == BsonType.NULL) {
        reader.readNull();
        value = null;
      } else {
        @SuppressWarnings("unchecked")
        final var codec = (Codec<Object>) registry.get(component.getType());

        value = decoderContext.decodeWithChildContext(codec, reader);
      }

      values.put(component.getName(), value);
    }

    reader.readEndDocument();

    try {
      final var args = Arrays.stream(components)
          .map(comp -> values.get(comp.getName()))
          .toArray();

      return constructor.newInstance(args);

    } catch (final ReflectiveOperationException exception) {
      throw new IllegalStateException(
          "Failed to decode " + clazz.getName(),
          exception);
    }
  }

  @NotNull
  private RecordComponent findComponent(@NotNull final String bsonName) {
    if ("_id".equals(bsonName)) {
      for (final var component : components) {
        if (component.isAnnotationPresent(Identifier.class)) {
          return component;
        }
      }
    }

    for (final var component : components) {
      if (component.isAnnotationPresent(Property.class)) {
        final var property = component.getAnnotation(Property.class);
        if (property.value().equals(bsonName)) {
          return component;
        }
      }
    }

    for (final var component : components) {
      if (component.getName().equals(bsonName)) {
        return component;
      }
    }

    throw new IllegalStateException(
        "Unknown field '" + bsonName + "' for " + clazz.getName());
  }

  @NotNull
  private String getFieldName(@NotNull final RecordComponent component) {
    if (component.isAnnotationPresent(Identifier.class)) {
      return "_id";
    }

    if (component.isAnnotationPresent(Property.class)) {
      final var property = component.getAnnotation(Property.class);
      return property.value();
    }

    return component.getName();
  }

  @Override
  @NotNull
  public Class<T> getEncoderClass() {
    return clazz;
  }
}
