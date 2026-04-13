package de.yyuh.celery.api.provider;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.libs.core.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface IProvider {

  @NotNull
  CompletableFuture<Result<Long, String>> connect(final @NotNull Credentials credentials);

  @NotNull
  CompletableFuture<Boolean> isConnected();

  void close();
}
