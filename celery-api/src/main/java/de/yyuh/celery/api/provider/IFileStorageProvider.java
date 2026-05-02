package de.yyuh.celery.api.provider;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.libs.core.result.Result;

/**
 * Provides database operations for a specific entity type.
 *
 * <p>
 * This interface defines the core CRUD operations that database
 * providers must implement. Operations are asynchronous and return
 * CompletableFuture for non-blocking execution.
 *
 * @param <K> the query type used for operations
 */
public interface IFileStorageProvider extends IProvider {

  @Override
  @NotNull
  CompletableFuture<Result<Long, String>> connect(final Credentials credentials);

  @NotNull
  CompletableFuture<Optional<File>> get(final String path, final String dest);

  @NotNull
  CompletableFuture<Void> save(final File file, final String path);

  @NotNull
  CompletableFuture<Void> delete(final String path);
}
