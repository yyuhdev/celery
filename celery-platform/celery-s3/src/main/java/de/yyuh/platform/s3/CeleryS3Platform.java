package de.yyuh.platform.s3;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.IProvider;

public final class CeleryS3Platform extends AbstractCeleryPlatform {

  public CeleryS3Platform() {
    super("s3", CeleryDatabaseType.S3, CeleryPlatformType.FILE_STORAGE);
  }

  @Override
  public IProvider defaultProvider() {
    return null;
  }
}
