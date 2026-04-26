package de.yyuh.celery.api.provider;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.libs.core.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Base interface for all providers.
 */
public interface IProvider {

  /**
   * Connects to the underlying service with the given credentials.
   *
   * @param credentials the credentials to use for connection
   * @return a CompletableFuture containing the connection time in milliseconds or an error
   */
  @NotNull
  CompletableFuture<Result<Long, String>> connect(final @NotNull Credentials credentials);

  /**
   * Checks if this provider is connected.
   *
   * @return a CompletableFuture containing the connection status
   */
  @NotNull
  CompletableFuture<Boolean> isConnected();

  /**
   * Closes this provider and releases resources.
   */
  void close();
}
