package de.yyuh.celery.api.migration;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a single database migration with versioning support.
 *
 * <p>Migrations are the fundamental unit of database schema changes.
 * Each migration has a unique version number and provides methods to
 * apply (up) and revert (down) the changes.
 */
public interface IMigration {

  /**
   * Returns the version number of this migration.
   *
   * @return the migration version
   */
  int version();

  /**
   * Returns a human-readable description of this migration.
   *
   * @return the migration description
   */
  @NotNull
  String description();

  /**
   * Applies the migration to the database.
   *
   * @return a CompletableFuture that completes when the migration has been applied
   */
  @NotNull
  CompletableFuture<Void> up();

  /**
   * Reverts the migration from the database.
   *
   * @return a CompletableFuture that completes when the migration has been reverted
   */
  @NotNull
  CompletableFuture<Void> down();
}
