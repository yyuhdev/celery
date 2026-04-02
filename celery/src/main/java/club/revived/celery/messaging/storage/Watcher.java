package club.revived.celery.messaging.storage;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import club.revived.celery.messaging.Concordia;
import club.revived.proto.v1.minigames.Cachable;

public final class Watcher {

  private final Gson gson = new GsonBuilder()
      .serializeNulls()
      .create();

  public <T> void watch(
      final @NotNull String key,
      final @NotNull Class<T> clazz,
      final @NotNull Consumer<T> callback) {
    Concordia.instance().pubSubProvider().subscribe(key + "updates", message -> {
      try {
        final var cachable = Cachable.parseFrom(message);
        final var json = cachable.getValue();
        final var object = gson.fromJson(json, clazz);
        callback.accept(object);
      } catch (final Exception e) {
        throw new RuntimeException("Error while trying to parse watched object", e);
      }
    });
  }
}
