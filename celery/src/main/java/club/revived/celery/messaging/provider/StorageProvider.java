package club.revived.celery.messaging.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import club.revived.celery.database.model.DatabaseCredentials;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {

  void connect(final @NotNull DatabaseCredentials credentials);

  @NotNull
  CompletableFuture<byte @Nullable []> get(final @NotNull String key);

  @NotNull
  CompletableFuture<Void> set(final @NotNull String key, final byte @NotNull [] value);

  @NotNull
  CompletableFuture<Void> set(final @NotNull String key, final byte @NotNull [] value, final long ttl);

  @NotNull
  CompletableFuture<Void> delete(final @NotNull String key);

  @NotNull
  CompletableFuture<List<String>> keys(final @NotNull String pattern);
}
