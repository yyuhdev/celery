package club.revived.celery.messaging.storage;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import club.revived.celery.messaging.Concordia;

public final class SyncableObject<T> {

  private final Concordia concordia = Concordia.instance();

  @NotNull
  private final String key;

  @NotNull
  private final Class<T> type;

  @Nullable
  private T current;

  @Nullable
  private Consumer<T> onUpdate;

  public SyncableObject(
      final @NotNull String key,
      final @NotNull Class<T> type) {
    this.key = key;
    this.type = type;
    initialize();
  }

  private void initialize() {
    concordia.watcher().watch(key, type, value -> {
      current = value;
      if (onUpdate != null) {
        onUpdate.accept(value);
      }
    });
    load();
  }

  public void load() {
    concordia.cacheManager().read(key, type).thenAccept(value ->
        value.ifPresent(v -> {
          if (onUpdate != null) {
            onUpdate.accept(v);
          }
        }));
  }

  @NotNull
  public String key() {
    return key;
  }

  @NotNull
  public T cached() {
    return current;
  }

  @Nullable
  public T get() {
    return current;
  }

  public void update(final @NotNull T value) {
    current = value;
    Concordia.instance().cacheManager().write(key, value);
  }

  public void onUpdate(final @NotNull Consumer<T> onUpdate) {
    this.onUpdate = onUpdate;
  }
}
