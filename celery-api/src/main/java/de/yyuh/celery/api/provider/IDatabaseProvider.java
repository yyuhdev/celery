package de.yyuh.celery.api.provider;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.libs.core.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Provides database operations for a specific entity type.
 *
 * <p>
 * This interface defines the core CRUD operations that database
 * providers must implement. Operations are asynchronous and return
 * CompletableFuture for non-blocking execution.
 *
 * @param <T> the entity type this provider handles
 * @param <K> the query type used for operations
 */
public interface IDatabaseProvider<T, K> extends IProvider {

  @Override
  @NotNull
  CompletableFuture<Result<Long, String>> connect(final Credentials credentials);

  /**
   * Retrieves a single entity matching the query.
   *
   * @param k the query specifying which entity to retrieve
   * @return a CompletableFuture containing the entity if found
   */
  @NotNull
  CompletableFuture<Optional<T>> get(final K k);

  /**
   * Finds all entities matching the query criteria.
   *
   * @param query the query specifying search criteria
   * @return a CompletableFuture containing the list of matching entities
   */
  @NotNull
  CompletableFuture<List<T>> find(final IQuery<T> query);

  /**
   * Retrieves all entities of the type.
   *
   * @return a CompletableFuture containing all entities
   */
  @NotNull
  CompletableFuture<List<T>> getAll();

  /**
   * Saves an entity, inserting or updating as appropriate.
   *
   * @param entity the entity to save
   * @return a CompletableFuture that completes when the save is finished
   */
  @NotNull
  CompletableFuture<Void> save(final T entity);

  /**
   * Deletes entities matching the query.
   *
   * @param k the query specifying which entities to delete
   * @return a CompletableFuture that completes when the deletion is finished
   */
  @NotNull
  CompletableFuture<Void> delete(final K k);
}
