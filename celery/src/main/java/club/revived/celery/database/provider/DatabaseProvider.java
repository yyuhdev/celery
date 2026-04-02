package club.revived.celery.database.provider;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import club.revived.celery.database.model.DatabaseCredentials;
import club.revived.celery.database.model.query.QueryFilter;

public interface DatabaseProvider<T> {

  void connect(final @NotNull DatabaseCredentials credentials);

  @NotNull
  CompletableFuture<Void> write(final @NotNull T entity);

  @NotNull
  CompletableFuture<List<T>> findAll();

  @NotNull
  CompletableFuture<List<T>> findAll(final @NotNull QueryFilter<T> filter);

  @NotNull
  CompletableFuture<Void> writeBatch(final @NotNull List<T> entities);

  @NotNull
  CompletableFuture<Optional<T>> find(final @NotNull QueryFilter<T> filter);

  @NotNull
  CompletableFuture<List<T>> findBatch(final @NotNull Collection<? extends QueryFilter<T>> filters);

  @NotNull
  CompletableFuture<Void> delete(final @NotNull QueryFilter<T> filter);
}
