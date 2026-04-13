package de.yyuh.celery.platform.mongodb;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.IProvider;
import de.yyuh.celery.api.provider.IDatabaseProvider;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.celery.platform.mongodb.provider.MongoDatabaseProvider;
import org.jetbrains.annotations.NotNull;

/**
 * MongoDB platform implementation for the Celery framework.
 *
 * <p>
 * This platform provides MongoDB-specific database operations
 * including storage capabilities with the MongoDatabaseProvider.
 */
public final class CeleryMongoDBPlatform extends AbstractCeleryPlatform {

  /**
   * Creates a new CeleryMongoDBPlatform instance.
   *
   * <p>
   * The platform is configured with MongoDB storage capabilities
   * and registered with the Celery framework.
   */
  public CeleryMongoDBPlatform() {
    super("mongodb", CeleryDatabaseType.MONGODB, CeleryPlatformType.STORAGE);
    registerProvider(IDatabaseProvider.class, new MongoDatabaseProvider());
  }

  @Override
  public @NotNull IProvider defaultProvider() {
    return provider(IDatabaseProvider.class).orElseThrow();
  }
}
