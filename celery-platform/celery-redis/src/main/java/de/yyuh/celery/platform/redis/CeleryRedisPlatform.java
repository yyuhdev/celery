package de.yyuh.celery.platform.redis;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.IProvider;
import de.yyuh.celery.platform.redis.provider.RedisMessagingProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Redis platform implementation for the Celery framework.
 *
 * <p>This platform connects to a single Redis instance and provides
 * Pub/Sub messaging via {@link RedisMessagingProvider}.
 * Messages are encoded as Protobuf byte arrays.
 */
public final class CeleryRedisPlatform extends AbstractCeleryPlatform {

  /**
   * Creates a new CeleryRedisPlatform with the Redis messaging provider.
   */
  public CeleryRedisPlatform() {
    super("redis", CeleryDatabaseType.REDIS, CeleryPlatformType.PUBSUB);

    registerProvider(IMessagingProvider.class, new RedisMessagingProvider());
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull IProvider defaultProvider() {
    return provider(IMessagingProvider.class).orElseThrow();
  }
}
