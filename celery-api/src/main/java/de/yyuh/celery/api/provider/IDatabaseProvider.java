package de.yyuh.celery.api.provider;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.libs.core.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IDatabaseProvider<T, K> {

    @NotNull
    CompletableFuture<Result<Long, String>> connect(final Credentials credentials);

    @NotNull
    CompletableFuture<Optional<T>> get(final K k);

    @NotNull
    CompletableFuture<List<T>> find(final IQuery<T> query);

    @NotNull
    CompletableFuture<List<T>> getAll();

    @NotNull
    CompletableFuture<Void> save(final T entity);

    @NotNull
    CompletableFuture<Void> delete(final K k);
}
