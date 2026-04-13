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
 * <p>
 * This platform provides Redis-specific messaging operations
 * including Pub/Sub capabilities with the RedisMessagingProvider.
 */
public final class CeleryRedisPlatform extends AbstractCeleryPlatform {

  public CeleryRedisPlatform() {
    super("redis", CeleryDatabaseType.REDIS, CeleryPlatformType.PUBSUB);
    registerProvider(IMessagingProvider.class, new RedisMessagingProvider());
  }

  @Override
  public @NotNull IProvider defaultProvider() {
    return provider(IMessagingProvider.class).orElseThrow();
  }
}
