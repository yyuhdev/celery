package de.yyuh.celery.api.migration;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class FileSqlMigration extends SqlMigration {

  private final Path upFile;
  private final Path downFile;
  private final Function<String, CompletableFuture<Void>> executor;

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
