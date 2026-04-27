package de.yyuh.celery.platform.redis.cache;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.ICacheProvider;
import de.yyuh.celery.api.provider.IProvider;
import org.jetbrains.annotations.NotNull;

public final class CeleryRedisCachePlatform extends AbstractCeleryPlatform {

  public CeleryRedisCachePlatform() {
    super("redis-cache", CeleryDatabaseType.REDIS, CeleryPlatformType.CACHE);

    registerProvider(ICacheProvider.class, new RedisCacheProvider());
  }

  @Override
  public @NotNull IProvider defaultProvider() {
    return provider(ICacheProvider.class).orElseThrow();
  }
}