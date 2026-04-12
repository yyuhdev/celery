package club.revived.celery.db;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import club.revived.celery.query.Query;

public interface IDatabaseProvider<T> {

  CompletableFuture<Void> save(final @NotNull T t);

  CompletableFuture<Optional<T>> get(final @NotNull Query query);

}
