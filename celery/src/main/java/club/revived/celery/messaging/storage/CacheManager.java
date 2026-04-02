package club.revived.celery.messaging.storage;

import club.revived.celery.Celery;
import club.revived.proto.v1.minigames.Cachable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class CacheManager {

  @NotNull
  public <T extends Message> CompletableFuture<Void> write(
      final @NotNull String key,
      final @NotNull T message) {
    final var cachable = Cachable.newBuilder()
        .setKey(key)
        .setValue(Base64.getEncoder().encodeToString(message.toByteArray()))
        .build();

    return Celery.instance().storage().set(key, cachable.toByteArray())
        .thenRun(() -> Celery.instance().pubSub().publish(key + ":updates", message.toByteArray()));
  }

  @NotNull
  public <T> CompletableFuture<Void> write(
      final @NotNull String key,
      final @NotNull T value) {
    final var json = Celery.instance().gson().toJson(value);
    final var cachable = Cachable.newBuilder()
        .setKey(key)
        .setValue(json)
        .build();

    final var data = cachable.toByteArray();
    final var updatePayload = json.getBytes();

    return Celery.instance().storage().set(key, data)
        .thenRun(() -> Celery.instance().pubSub().publish(key + ":updates", updatePayload));
  }

  @NotNull
  public <T> CompletableFuture<Void> write(
      final @NotNull String key,
      final @NotNull T value,
      final long ttl) {
    final var json = Celery.instance().gson().toJson(value);
    final var cachable = Cachable.newBuilder()
        .setKey(key)
        .setValue(json)
        .build();

    final var data = cachable.toByteArray();
    final var updatePayload = json.getBytes();

    return Celery.instance().storage().set(key, data, ttl)
        .thenRun(() -> Celery.instance().pubSub().publish(key + ":updates", updatePayload));
  }

  @NotNull
  public <T> CompletableFuture<Optional<T>> read(
      final @NotNull String key,
      final @NotNull Class<T> type) {
    if (Message.class.isAssignableFrom(type)) {
      throw new IllegalArgumentException("Use read(key, type, parser) for Protobuf messages");
    }

    return Celery.instance().storage().get(key).thenApply(data -> {
      if (data == null) {
        return Optional.empty();
      }

      try {
        final var cachable = Cachable.parseFrom(data);
        final T value = Celery.instance().gson().fromJson(cachable.getValue(), type);
        return Optional.ofNullable(value);
      } catch (final Exception e) {
        throw new RuntimeException("Failed to deserialize cached value", e);
      }
    });
  }

  @NotNull
  public <T> CompletableFuture<@Nullable T> read(
      final @NotNull String key,
      final @NotNull Class<T> type,
      final @Nullable Parser<T> parser) {
    return Celery.instance().storage().get(key).thenApply(data -> {
      if (data == null) {
        return null;
      }

      try {
        final var cachable = Cachable.parseFrom(data);

        if (Message.class.isAssignableFrom(type)) {
          if (parser == null) {
            throw new IllegalArgumentException("Parser is required for Protobuf messages");
          }

          final var payload = Base64.getDecoder().decode(cachable.getValue());
          return parser.parseFrom(payload);
        }

        return Celery.instance().gson().fromJson(cachable.getValue(), type);
      } catch (final Exception e) {
        throw new RuntimeException("Failed to read cached value", e);
      }
    });
  }

  @NotNull
  public <T> CompletableFuture<List<T>> batchRead(
      final @NotNull List<String> keys,
      final @NotNull Class<T> type) {
    final List<CompletableFuture<Optional<T>>> futures = keys.stream()
        .map(key -> this.read(key, type))
        .toList();

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(_ -> futures.stream()
            .map(CompletableFuture::join)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList());
  }

  @NotNull
  public CompletableFuture<Void> delete(final @NotNull String key) {
    return Celery.instance().storage().delete(key)
        .thenRun(() -> Celery.instance().pubSub().publish(key + ":updates", new byte[0]));
  }

  @NotNull
  public CompletableFuture<Void> batchWrite(final @NotNull List<Entry<?>> entries) {
    final var futures = entries.stream()
        .map(entry -> this.write(entry.key(), entry.value()))
        .toList();

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
  }

  public record Entry<T>(
      @NotNull String key,
      @NotNull T value) {
  }
}
