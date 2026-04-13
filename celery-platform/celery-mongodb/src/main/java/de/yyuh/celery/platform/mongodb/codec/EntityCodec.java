package de.yyuh.celery.platform.mongodb.codec;

import de.yyuh.celery.api.annotation.Field;
import de.yyuh.celery.api.annotation.Identifier;
import de.yyuh.celery.api.annotation.Ignore;
import de.yyuh.celery.api.entity.IEntity;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.Map;

/**
 * MongoDB Codec for encoding and decoding IEntity record types.
 *
 * <p>This codec handles the conversion between Java record components
 * and MongoDB BSON documents, respecting @Identifier, @Field, and
 * @Ignore annotations.
 *
 * @param <T> the entity type, must be a record
 */
public final class EntityCodec<T extends IEntity> implements Codec<T> {

  private final Class<T> clazz;
  private final CodecRegistry registry;
  private final Constructor<T> constructor;
  private final RecordComponent[] components;

  /**
   * Creates a new EntityCodec for the specified record class.
   *
   * @param clazz the entity record class
   * @param registry the codec registry for nested types
   * @throws IllegalArgumentException if the class is not a record
   */
  public EntityCodec(
      @NotNull final Class<T> clazz,
      @NotNull final CodecRegistry registry) {

    if (!clazz.isRecord()) {
      throw new IllegalArgumentException("EntityCodec supports records only. Class: " + clazz.getName());
    }

    this.clazz = clazz;
    this.registry = registry;
    this.components = clazz.getRecordComponents();

    try {
      final Class<?>[] parameterTypes = new Class<?>[components.length];

      for (int i = 0; i < components.length; i++) {
        parameterTypes[i] = components[i].getType();
      }

      this.constructor = clazz.getDeclaredConstructor(parameterTypes);
      this.constructor.setAccessible(true);

    } catch (final Exception exception) {
      throw new RuntimeException(
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

      for (final RecordComponent component : components) {
        if (component.isAnnotationPresent(Ignore.class)) {
          continue;
        }

        final String fieldName = this.getFieldName(component);
        final Object fieldValue = component.getAccessor().invoke(value);

        if (fieldValue == null) {
          writer.writeName(fieldName);
          writer.writeNull();
          continue;
        }

        writer.writeName(fieldName);
        @SuppressWarnings("unchecked")
        final Codec<Object> codec = (Codec<Object>) registry.get(fieldValue.getClass());

        encoderContext.encodeWithChildContext(codec, writer, fieldValue);
      }

    } catch (final Exception exception) {
      throw new RuntimeException(
          "Failed to encode " + clazz.getName(),
          exception);
    }

    writer.writeEndDocument();
  }

  @Override
  public T decode(
      @NotNull final BsonReader reader,
      @NotNull final DecoderContext decoderContext) {

    final Map<String, Object> values = new HashMap<>();

    reader.readStartDocument();

    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

      final String bsonName = reader.readName();

      final RecordComponent component = findComponent(bsonName);

      if (component == null) {
        reader.skipValue();
        continue;
      }

      final Object value;

      if (reader.getCurrentBsonType() == BsonType.NULL) {
        reader.readNull();
        value = null;
      } else {
        @SuppressWarnings("unchecked")
        final Codec<Object> codec = (Codec<Object>) registry.get(component.getType());

        value = decoderContext.decodeWithChildContext(codec, reader);
      }

      values.put(component.getName(), value);
    }

    reader.readEndDocument();

    try {
      final Object[] args = new Object[components.length];

      for (int i = 0; i < components.length; i++) {
        args[i] = values.get(components[i].getName());
      }

      return constructor.newInstance(args);

    } catch (final Exception exception) {
      throw new RuntimeException(
          "Failed to decode " + clazz.getName(),
          exception);
    }
  }

  /**
   * Finds the record component matching a BSON field name.
   *
   * <p>First checks for the special "_id" field mapped to @Identifier,
   * then checks for @Field annotations, and finally falls back to
   * matching by Java field name.
   *
   * @param bsonName the BSON field name to match
   * @return the matching RecordComponent, or null if not found
   */
  private RecordComponent findComponent(@NotNull final String bsonName) {

    if ("_id".equals(bsonName)) {
      for (final RecordComponent component : components) {
        if (component.isAnnotationPresent(Identifier.class))
          return component;
      }
    }

    for (final RecordComponent component : components) {
      if (component.isAnnotationPresent(Field.class)) {
        if (component.getAnnotation(Field.class).value().equals(bsonName)) {
          return component;
        }
      }
    }

    for (final RecordComponent component : components) {
      if (component.getName().equals(bsonName))
        return component;
    }

    return null;
  }

  /**
   * Returns the database field name for a record component.
   *
   * <p>Returns "_id" for @Identifier, the @Field value for @Field,
   * or the component's Java name as a fallback.
   *
   * @param component the record component to get the field name for
   * @return the database field name
   */
  @NotNull
  private String getFieldName(final RecordComponent component) {
    if (component.isAnnotationPresent(Identifier.class)) {
      return "_id";
    }

    if (component.isAnnotationPresent(Field.class)) {
      return component.getAnnotation(Field.class).value();
    }

    return component.getName();
  }

  @Override
  public Class<T> getEncoderClass() {
    return clazz;
  }
}
