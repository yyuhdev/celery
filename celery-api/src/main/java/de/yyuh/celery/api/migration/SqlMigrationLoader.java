package de.yyuh.celery.api.migration;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Loads SQL migrations from a directory based on naming conventions.
 *
 * <p>Migration files must follow the naming pattern:
 * {@code V{version}__{description}.(up|down).sql}
 *
 * <p>For each up migration found, the loader looks for a corresponding
 * down migration with the same version and description.
 *
 * @see FileSqlMigration
 * @see MigrationManager
 */
public final class SqlMigrationLoader {

  private static final Pattern FILENAME_PATTERN = Pattern.compile("V(\\d+)__(.+)\\.(up|down)\\.sql");

  private final MigrationManager migrationManager;
  private final Function<String, CompletableFuture<Void>> sqlExecutor;

  /**
   * Creates a new SqlMigrationLoader.
   *
   * @param migrationManager the manager to register loaded migrations
   * @param sqlExecutor the function to execute SQL statements
   */
  public SqlMigrationLoader(@NotNull MigrationManager migrationManager,
      @NotNull Function<String, CompletableFuture<Void>> sqlExecutor) {
    this.migrationManager = migrationManager;
    this.sqlExecutor = sqlExecutor;
  }

  /**
   * Loads all migrations from the specified directory.
   *
   * <p>Only files ending with {@code .up.sql} are considered.
   * Each up migration must have a corresponding down migration.
   *
   * @param directory the directory containing migration files
   * @throws IOException if the directory cannot be read
   * @throws IllegalArgumentException if the path is not a directory
   * @throws RuntimeException if a down migration is missing
   */
  public void loadFromDirectory(@NotNull Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      throw new IllegalArgumentException("Path is not a directory: " + directory);
    }

    try (Stream<Path> files = Files.list(directory)) {
      files.filter(f -> f.toString().endsWith(".up.sql"))
          .forEach(this::processUpFile);
    }
  }

  private void processUpFile(Path upFile) {
    String filename = upFile.getFileName().toString();
    Matcher matcher = FILENAME_PATTERN.matcher(filename);

    if (matcher.matches()) {
      int version = Integer.parseInt(matcher.group(1));
      String description = matcher.group(2).replace("_", " ");
      Path downFile = upFile.resolveSibling(filename.replace(".up.sql", ".down.sql"));

      if (Files.exists(downFile)) {
        migrationManager.register(new FileSqlMigration(version, description, upFile, downFile, sqlExecutor));
      } else {
        throw new RuntimeException("Missing down migration for version " + version + ": " + downFile);
      }
    }
  }
}
