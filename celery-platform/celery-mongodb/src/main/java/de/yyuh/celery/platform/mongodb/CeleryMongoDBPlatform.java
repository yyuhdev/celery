package de.yyuh.celery.platform.mongodb;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.IDatabaseProvider;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.celery.platform.mongodb.provider.MongoDatabaseProvider;
import org.jetbrains.annotations.NotNull;

public final class CeleryMongoDBPlatform extends AbstractCeleryPlatform {

  private final IDatabaseProvider<IEntity, IQuery> provider = new MongoDatabaseProvider();

  public CeleryMongoDBPlatform() {
    super("mongodb", CeleryDatabaseType.MONGODB, CeleryPlatformType.STORAGE);
  }

  @Override
  public @NotNull IDatabaseProvider<IEntity, IQuery> defaultProvider() {
    return this.provider;
  }
}
