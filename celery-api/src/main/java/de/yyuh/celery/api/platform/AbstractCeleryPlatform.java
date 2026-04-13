package de.yyuh.celery.api.platform;

import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.IDatabaseType;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.provider.IDatabaseProvider;
import de.yyuh.celery.api.query.IQuery;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for platform implementations.
 *
 * <p>A platform represents a specific database or service type (e.g., MongoDB, Redis)
 * and provides access to its default database provider.
 */
public abstract class AbstractCeleryPlatform {

  private final String id;
  private final IDatabaseType databaseType;
  private final CeleryPlatformType celeryPlatformType;

  /**
   * Creates a new platform with the specified configuration.
   *
   * @param id the unique identifier for this platform instance
   * @param databaseType the type of database this platform supports
   * @param platformType the platform type category
   */
  public AbstractCeleryPlatform(
      final @NotNull String id,
      final @NotNull IDatabaseType databaseType,
      final @NotNull CeleryPlatformType platformType) {
    this.id = id;
    this.databaseType = databaseType;
    this.celeryPlatformType = platformType;
  }

  /**
   * Returns the unique identifier of this platform.
   *
   * @return the platform ID
   */
  @NotNull
  public String getId() {
    return this.id;
  }

  /**
   * Returns the database type this platform supports.
   *
   * @return the database type
   */
  @NotNull
  public IDatabaseType getDatabaseType() {
    return this.databaseType;
  }

  /**
   * Returns the platform type category.
   *
   * @return the platform type
   */
  @NotNull
  public CeleryPlatformType getCeleryPlatformType() {
    return this.celeryPlatformType;
  }

  /**
   * Returns the default database provider for this platform.
   *
   * @return the default database provider
   */
  public abstract IDatabaseProvider<IEntity, IQuery> defaultProvider();
}
