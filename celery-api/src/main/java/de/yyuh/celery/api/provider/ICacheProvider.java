package de.yyuh.celery.api.provider;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ICacheProvider extends IProvider {

  @NotNull
  CompletableFuture<Void> set(final @NotNull String key, final byte @NotNull [] value, final @NotNull Duration ttl);

  @NotNull
  CompletableFuture<Optional<byte[]>> get(final @NotNull String key);

  @NotNull
  CompletableFuture<Void> delete(final @NotNull String key);

  @NotNull
  CompletableFuture<Boolean> exists(final @NotNull String key);
}