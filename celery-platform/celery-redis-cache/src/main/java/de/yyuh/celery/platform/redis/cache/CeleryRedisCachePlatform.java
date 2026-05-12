package de.yyuh.celery.platform.redis.cache;

import org.jetbrains.annotations.NotNull;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.ICacheProvider;

/**
 * Redis cache platform implementation for the Celery framework.
 *
 * <p>
 * This platform connects to a single Redis instance and provides
 * cache operations (get, set, delete, exists) via {@link RedisCacheProvider}.
 */
public final class CeleryRedisCachePlatform extends AbstractCeleryPlatform {

  /**
   * Creates a new CeleryRedisCachePlatform with the Redis cache provider.
   */
  public CeleryRedisCachePlatform() {
    super("redis-cache", CeleryDatabaseType.REDIS, CeleryPlatformType.CACHE);

    registerProvider(ICacheProvider.class, new RedisCacheProvider());
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull ICacheProvider defaultProvider() {
    return provider(ICacheProvider.class).orElseThrow();
  }
}
