package de.yyuh.celery.api.migration;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class MigrationManager {

  private final Map<Integer, IMigration> migrations = new TreeMap<>();

  public void register(@NotNull IMigration migration) {
    if (migrations.containsKey(migration.version())) {
      throw new IllegalArgumentException("Migration with version " + migration.version() + " already registered");
    }
    migrations.put(migration.version(), migration);
  }

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
