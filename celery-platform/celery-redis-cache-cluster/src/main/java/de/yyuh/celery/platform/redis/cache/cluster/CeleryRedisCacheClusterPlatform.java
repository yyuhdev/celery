package de.yyuh.celery.platform.redis.cache.cluster;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.ICacheProvider;
import de.yyuh.celery.api.provider.IProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Redis Cluster cache platform with automatic sharding.
 *
 * <p>
 * This platform uses {@link RedisClusterCacheProvider} to provide
 * cluster-aware cache operations. Keys are automatically sharded across
 * the Redis Cluster using hash slots, enabling horizontal scaling of
 * cache storage.
 */
public final class CeleryRedisCacheClusterPlatform extends AbstractCeleryPlatform {

  public CeleryRedisCacheClusterPlatform() {
    super("redis-cache-cluster", CeleryDatabaseType.REDIS, CeleryPlatformType.CACHE);

    registerProvider(ICacheProvider.class, new RedisClusterCacheProvider());
  }

  @Override
  public @NotNull IProvider defaultProvider() {
    return provider(ICacheProvider.class).orElseThrow();
  }
}
