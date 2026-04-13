package de.yyuh.celery.api.migration;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Manages database migrations with support for versioning, applying, and reverting.
 *
 * <p>MigrationManager maintains a registry of migrations and handles sequential
 * execution of migrations when moving between versions.
 *
 * @see IMigration
 */
public final class MigrationManager {

  private final Map<Integer, IMigration> migrations = new TreeMap<>();

  /**
   * Registers a migration with the manager.
   *
   * @param migration the migration to register
   * @throws IllegalArgumentException if a migration with the same version is already registered
   */
  public void register(@NotNull IMigration migration) {
    if (migrations.containsKey(migration.version())) {
      throw new IllegalArgumentException("Migration with version " + migration.version() + " already registered");
    }
    migrations.put(migration.version(), migration);
  }

  /**
   * Migrates from the current version to the target version.
   *
   * <p>If targetVersion is greater than currentVersion, applies all migrations
   * between the two versions in order. If targetVersion is less than currentVersion,
   * reverts all migrations in reverse order.
   *
   * @param targetVersion the version to migrate to
   * @param currentVersion the current database version
   * @return a CompletableFuture that completes when all migrations have been applied or reverted
   */
  @NotNull
  public CompletableFuture<Void> migrateTo(final int targetVersion, final int currentVersion) {
    if (currentVersion < targetVersion) {
      final List<IMigration> toApply = migrations.values().stream()
          .filter(m -> m.version() > currentVersion && m.version() <= targetVersion)
          .sorted(Comparator.comparingInt(IMigration::version))
          .collect(Collectors.toList());

      CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
      for (IMigration m : toApply) {
        future = future.thenCompose(v -> m.up());
      }
      return future;
    } else if (currentVersion > targetVersion) {
      final List<IMigration> toRevert = migrations.values().stream()
          .filter(m -> m.version() <= currentVersion && m.version() > targetVersion)
          .sorted(Comparator.comparingInt(IMigration::version).reversed())
          .collect(Collectors.toList());

      CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
      for (IMigration m : toRevert) {
        future = future.thenCompose(v -> m.down());
      }
      return future;
    }
    return CompletableFuture.completedFuture(null);
  }
}
