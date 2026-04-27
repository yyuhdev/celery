package de.yyuh.celery.api.provider;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.entity.ILogEntry;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.libs.core.result.Result;

public interface ITimeseriesProvider extends IProvider {

  @Override
  @NotNull
  CompletableFuture<Result<Long, String>> connect(final Credentials credentials);

  @NotNull
  CompletableFuture<List<ILogEntry>> find(final IQuery<ILogEntry> query);

  @NotNull
  CompletableFuture<Void> save(final ILogEntry entity);

  @NotNull
  CompletableFuture<Void> delete(final Instant timestamp);

}
