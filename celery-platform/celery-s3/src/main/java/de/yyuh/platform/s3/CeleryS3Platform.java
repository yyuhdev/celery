package de.yyuh.platform.s3;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.IFileStorageProvider;
import de.yyuh.celery.api.provider.IProvider;
import de.yyuh.platform.s3.provider.S3FileStorageProvider;

public final class CeleryS3Platform extends AbstractCeleryPlatform {

  public CeleryS3Platform() {
    super("s3", CeleryDatabaseType.S3, CeleryPlatformType.FILE_STORAGE);

    this.registerProvider(IFileStorageProvider.class, new S3FileStorageProvider());
  }

  @Override
  public IProvider defaultProvider() {
    return this.provider(IFileStorageProvider.class).orElseThrow();
  }
}
