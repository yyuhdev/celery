package club.revived.celery.database.codec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import club.revived.celery.database.model.Entity;

public final class EntityCodecProvider implements CodecProvider {

  @Override
  @SuppressWarnings("unchecked")
  @Nullable
  public <T> Codec<T> get(
      @NotNull final Class<T> clazz,
      @NotNull final CodecRegistry registry) {

    if (!Entity.class.isAssignableFrom(clazz)) {
      return null;
    }

    if (!clazz.isRecord()) {
      return null;
    }

    return (Codec<T>) new EntityCodec<>(
        (Class<? extends Entity>) clazz,
        registry);
  }
}
