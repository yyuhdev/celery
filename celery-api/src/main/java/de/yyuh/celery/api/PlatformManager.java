package de.yyuh.celery.api;

import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.IDatabaseProvider;
import de.yyuh.celery.api.query.IQuery;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public final class PlatformManager {

  private static PlatformManager instance;

  public static PlatformManager getInstance() {
    if (instance == null) {
      throw new IllegalStateException("DatabaseManager is not registered");
    }

    return instance;
  }

  private final List<AbstractCeleryPlatform> platformInstances;

  public PlatformManager(final @NotNull List<AbstractCeleryPlatform> platformInstances) {
    this.platformInstances = platformInstances;

    instance = this;
  }

  @NotNull
  public Optional<AbstractCeleryPlatform> getPlatform(final @NotNull String id) {
    return this.platformInstances.stream()
        .filter(platform -> platform.getId().equals(id))
        .findFirst();
  }

  @NotNull
  public Optional<AbstractCeleryPlatform> getDefaultPlatform(final @NotNull IDatabaseType databaseType) {
    final var defaultPlatform = databaseType.defaultPlatform();

    return this.platformInstances.stream()
        .filter(platform -> platform.getCeleryPlatformType() == defaultPlatform)
        .findFirst();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T extends IEntity, K extends IQuery> Optional<IDatabaseProvider<T, K>> getProvider(
      final @NotNull CeleryPlatformType type, final @NotNull IDatabaseType databaseType) {
    return this.platformInstances.stream()
        .filter(platform -> platform.getCeleryPlatformType() == type && platform.getDatabaseType() == databaseType)
        .map(platform -> (IDatabaseProvider<T, K>) platform.defaultProvider())
        .findFirst();
  }
}
