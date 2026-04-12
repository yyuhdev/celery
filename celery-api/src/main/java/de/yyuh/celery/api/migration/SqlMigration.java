package de.yyuh.celery.api.migration;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class SqlMigration implements IMigration {

  private final int version;
  private final String description;

  protected SqlMigration(int version, @NotNull String description) {
    this.version = version;
    this.description = description;
  }

  @Override
  public int version() {
    return version;
  }

  @Override
  public @NotNull String description() {
    return description;
  }

  // Concrete implementations will provide the SQL execution logic
  protected abstract CompletableFuture<Void> executeSql(String sql);

  public abstract String upSql();

  public abstract String downSql();

  @Override
  public CompletableFuture<Void> up() {
    return executeSql(upSql());
  }

  @Override
  public CompletableFuture<Void> down() {
    return executeSql(downSql());
  }
}
