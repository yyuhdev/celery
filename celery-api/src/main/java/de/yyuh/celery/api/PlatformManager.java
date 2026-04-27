package de.yyuh.celery.api;

import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.IDatabaseProvider;
import de.yyuh.celery.api.query.IQuery;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Manages registered platforms and provides lookup capabilities.
 *
 * <p>
 * PlatformManager is a singleton that maintains a registry of
 * platform instances and provides methods to find platforms and
 * their associated database providers.
 */
public final class PlatformManager {

  private static PlatformManager instance;

  /**
   * Returns the singleton instance.
   *
   * @return the PlatformManager instance
   * @throws IllegalStateException if no instance has been registered
   */
  public static PlatformManager getInstance() {
    if (instance == null) {
      throw new IllegalStateException("DatabaseManager is not registered");
    }

    return instance;
  }

  private final List<AbstractCeleryPlatform> platformInstances;

  /**
   * Creates a new PlatformManager and registers it as the singleton instance.
   *
   * @param platformInstances the list of platforms to manage
   */
  public PlatformManager(final @NotNull List<AbstractCeleryPlatform> platformInstances) {
    this.platformInstances = platformInstances;

    instance = this;
  }

  /**
   * Finds a platform by its unique identifier.
   *
   * @param id the platform ID to search for
   * @return an Optional containing the platform if found
   */
  @NotNull
  public Optional<AbstractCeleryPlatform> getPlatform(final @NotNull String id) {
    return this.platformInstances.stream()
        .filter(platform -> platform.getId().equals(id))
        .findFirst();
  }

  @NotNull
  public Optional<AbstractCeleryPlatform> getPlatform(
      final @NotNull Class<? extends AbstractCeleryPlatform> platformClass) {
    return this.platformInstances.stream()
        .filter(platform -> platform.getClass().equals(platformClass))
        .findFirst();
  }

  /**
   * Finds the default platform for a database type.
   *
   * @param databaseType the database type to find a platform for
   * @return an Optional containing the default platform if available
   */
  @NotNull
  public Optional<AbstractCeleryPlatform> getDefaultPlatform(final @NotNull IDatabaseType databaseType) {
    final var defaultPlatform = databaseType.defaultPlatform();

    return this.platformInstances.stream()
        .filter(platform -> platform.getCeleryPlatformType() == defaultPlatform)
        .findFirst();
  }

  /**
   * Finds the database provider for a platform and database type combination.
   *
   * @param type         the platform type
   * @param databaseType the database type
   * @param <T>          the entity type
   * @param <K>          the query type
   * @return an Optional containing the provider if found
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public <T extends IEntity, K extends IQuery<IEntity>> Optional<IDatabaseProvider<T, K>> getProvider(
      final @NotNull CeleryPlatformType type, final @NotNull IDatabaseType databaseType) {
    return this.platformInstances.stream()
        .filter(platform -> platform.getCeleryPlatformType() == type && platform.getDatabaseType() == databaseType)
        .map(platform -> (IDatabaseProvider<T, K>) platform.defaultProvider())
        .findFirst();
  }
}
