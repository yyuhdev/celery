package de.yyuh.celery.api.provider;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.entity.ILogEntry;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.libs.core.result.Result;

/**
 * A provider for storing and querying time-series log data.
 *
 * <p>
 * This interface extends {@link IProvider} to define the contract for backends
 * that persist {@link ILogEntry} entities indexed by time. Implementations are
 * expected to handle the details of connecting to a specific time-series
 * database
 * or storage system (e.g. InfluxDB, TimescaleDB), while exposing a uniform API
 * for
 * CRUD operations.
 *
 * <p>
 * All operations return {@link CompletableFuture}, allowing implementations to
 * perform I/O asynchronously without blocking the calling thread.
 *
 * <h3>Typical usage</h3>
 *
 * <pre>{@code
 *   ITimeseriesProvider provider = // obtain implementation
 *   provider.connect(credentials)
 *       .thenCompose(result -> provider.find(query))
 *       .thenAccept(entries -> { ... });
 * }</pre>
 *
 * @see IProvider
 * @see ILogEntry
 * @see IQuery
 */
public interface ITimeseriesProvider extends IProvider {

  /**
   * Connects to the time-series backend using the given credentials.
   *
   * <p>
   * Override of {@link IProvider#connect(Credentials)}. Implementations should
   * establish and authenticate a connection to the underlying time-series
   * database
   * and return a {@link Result} indicating either the connection latency in
   * milliseconds or an error description.
   *
   * @param credentials the credentials to authenticate with
   * @return a CompletableFuture that completes with the connection time in
   *         milliseconds on success, or an error string on failure
   */
  @Override
  @NotNull
  CompletableFuture<Result<Long, String>> connect(final Credentials credentials);

  /**
   * Queries log entries that match the given query specification.
   *
   * <p>
   * The query may include filters (key-value conditions), a limit on the
   * number of results, and an offset for pagination. Implementations translate
   * the abstract query into the native query language of the backing store.
   *
   * @param query the query describing which log entries to retrieve; must not
   *              be {@code null}
   * @return a CompletableFuture that completes with a list of matching
   *         {@link ILogEntry} instances (possibly empty, never {@code null})
   */
  @NotNull
  CompletableFuture<List<ILogEntry>> find(final IQuery<ILogEntry> query);

  /**
   * Persists a single log entry into the time-series store.
   *
   * <p>
   * The entry is typically written immediately or batched for efficiency;
   * the returned future completes once the write has been acknowledged by the
   * underlying system.
   *
   * @param entity the log entry to persist; must not be {@code null}
   * @return a CompletableFuture that completes when the entry has been saved
   */
  @NotNull
  CompletableFuture<Void> save(final ILogEntry entity);

  /**
   * Deletes log entries associated with the given timestamp.
   *
   * <p>
   * Implementations should remove all entries whose recorded time matches
   * the supplied {@link Instant}. The exact matching granularity (exact instant
   * vs. time range) is implementation-defined.
   *
   * @param timestamp the instant whose associated log entries should be removed;
   *                  must not be {@code null}
   * @return a CompletableFuture that completes when the deletion has been
   *         acknowledged by the underlying system
   */
  @NotNull
  CompletableFuture<Void> delete(final Instant timestamp);

}
