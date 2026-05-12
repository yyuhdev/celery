package de.yyuh.celery.platform.mongodb;

import org.jetbrains.annotations.NotNull;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.IDatabaseProvider;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.celery.platform.mongodb.provider.MongoDatabaseProvider;

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

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("unchecked")
  public @NotNull IDatabaseProvider<IEntity, IQuery<IEntity>> defaultProvider() {
    return provider(IDatabaseProvider.class).orElseThrow();
  }
}
