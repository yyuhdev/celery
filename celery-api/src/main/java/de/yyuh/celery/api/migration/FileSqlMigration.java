package de.yyuh.celery.api.migration;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A SQL-based migration that reads SQL statements from external files.
 *
 * <p>This implementation loads the up and down SQL scripts from filesystem paths,
 * making it suitable for file-based migration management.
 *
 * @see SqlMigration
 */
public final class FileSqlMigration extends SqlMigration {

  private final Path upFile;
  private final Path downFile;
  private final Function<String, CompletableFuture<Void>> executor;

  /**
   * Creates a new FileSqlMigration.
   *
   * @param version the migration version number
   * @param description a human-readable description of the migration
   * @param upFile the path to the file containing the up migration SQL
   * @param downFile the path to the file containing the down migration SQL
   * @param executor the function to execute SQL statements
   */
  public FileSqlMigration(
      final int version,
      final @NotNull String description,
      final @NotNull Path upFile,
      final @NotNull Path downFile,
      final @NotNull Function<String, CompletableFuture<Void>> executor) {
    super(version, description);
    this.upFile = upFile;
    this.downFile = downFile;
    this.executor = executor;
  }

  @Override
  protected CompletableFuture<Void> executeSql(final @NotNull String sql) {
    return executor.apply(sql);
  }

  @Override
  public String upSql() {
    return readFile(upFile);
  }

  @Override
  public String downSql() {
    return readFile(downFile);
  }

  private String readFile(final @NotNull Path path) {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      throw new RuntimeException("Could not read SQL file: " + path, e);
    }
  }
}
