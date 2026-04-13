package de.yyuh.celery.api.migration;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Abstract base implementation of IMigration for SQL-based migrations.
 *
 * <p>Subclasses must implement executeSql to provide database-specific
 * SQL execution, while upSql and downSql provide the actual SQL statements.
 *
 * @see IMigration
 */
public abstract class SqlMigration implements IMigration {

  private final int version;
  private final String description;

  /**
   * Creates a new SqlMigration.
   *
   * @param version the migration version number
   * @param description a human-readable description of the migration
   */
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

  /**
   * Executes a SQL statement against the target database.
   *
   * @param sql the SQL statement to execute
   * @return a CompletableFuture that completes when the statement has been executed
   */
  protected abstract CompletableFuture<Void> executeSql(String sql);

  /**
   * Returns the SQL statement(s) for applying this migration.
   *
   * @return the up migration SQL
   */
  public abstract String upSql();

  /**
   * Returns the SQL statement(s) for reverting this migration.
   *
   * @return the down migration SQL
   */
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
