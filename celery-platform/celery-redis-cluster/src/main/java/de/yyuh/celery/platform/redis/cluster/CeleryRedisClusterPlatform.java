package de.yyuh.celery.platform.redis.cluster;

import org.jetbrains.annotations.NotNull;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.platform.redis.cluster.provider.RedisClusterMessagingProvider;

/**
 * Redis Cluster platform for Pub/Sub messaging.
 *
 * <p>
 * This platform uses {@link RedisClusterMessagingProvider} to provide
 * cluster-aware Redis Pub/Sub. Messages are sharded across the cluster
 * and subscribers receive messages regardless of which node they are
 * connected to.
 */
public final class CeleryRedisClusterPlatform extends AbstractCeleryPlatform {

  public CeleryRedisClusterPlatform() {
    super("redis-cluster", CeleryDatabaseType.REDIS, CeleryPlatformType.PUBSUB);

    registerProvider(IMessagingProvider.class, new RedisClusterMessagingProvider());
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull IMessagingProvider defaultProvider() {
    return provider(IMessagingProvider.class).orElseThrow();
  }
}
