package de.yyuh.celery.platform.mongodb.codec;

import de.yyuh.celery.api.entity.IEntity;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.jetbrains.annotations.NotNull;

public final class EntityCodecProvider implements CodecProvider {

  @Override
  @SuppressWarnings("unchecked")
  public <T> Codec<T> get(final @NotNull Class<T> clazz, final @NotNull CodecRegistry registry) {
    if (IEntity.class.isAssignableFrom(clazz) && clazz.isRecord()) {
      return (Codec<T>) new EntityCodec(clazz, registry);
    }

    return null;
  }
}
