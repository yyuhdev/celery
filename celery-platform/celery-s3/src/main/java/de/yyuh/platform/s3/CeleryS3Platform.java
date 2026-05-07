package de.yyuh.platform.s3;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.IFileStorageProvider;
import de.yyuh.celery.api.provider.IProvider;
import de.yyuh.platform.s3.provider.S3FileStorageProvider;

/**
 * Amazon S3 platform implementation for the Celery framework.
 *
 * <p>This platform connects to Amazon S3 (or S3-compatible storage) and
 * provides file storage operations via {@link S3FileStorageProvider}.
 */
public final class CeleryS3Platform extends AbstractCeleryPlatform {

  /**
   * Creates a new CeleryS3Platform with the S3 file storage provider.
   */
  public CeleryS3Platform() {
    super("s3", CeleryDatabaseType.S3, CeleryPlatformType.FILE_STORAGE);

    this.registerProvider(IFileStorageProvider.class, new S3FileStorageProvider());
  }

  /** {@inheritDoc} */
  @Override
  public IProvider defaultProvider() {
    return this.provider(IFileStorageProvider.class).orElseThrow();
  }
}
