package club.revived.celery.database.provider;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import club.revived.celery.database.model.DatabaseCredentials;
import club.revived.celery.database.model.query.QueryFilter;

@SuppressWarnings("unchecked")
public final class DatabaseRegistry {

  private final Map<Class<?>, DatabaseProvider<?>> providers = new ConcurrentHashMap<>();

  public <T> void register(
      final @NotNull Class<T> type,
      final @NotNull DatabaseProvider<T> provider,
      final @NotNull DatabaseCredentials credentials) {
    provider.connect(credentials);
    providers.put(type, provider);
  }

  public <T> void unregister(final @NotNull Class<T> type) {
    providers.remove(type);
  }

  @NotNull
  public <T> CompletableFuture<Void> write(
      final @NotNull Class<T> type,
      final @NotNull T entity) {
    return this.<T>resolve(type).write(entity);
  }

  @NotNull
  public <T> CompletableFuture<Void> writeBatch(
      final @NotNull Class<T> type,
      final @NotNull List<T> entities) {
    return this.<T>resolve(type).writeBatch(entities);
  }

  @NotNull
  public <T> CompletableFuture<Optional<T>> find(
      final @NotNull Class<T> type,
      final @NotNull QueryFilter<T> filter) {
    return this.<T>resolve(type).find(filter);
  }

  @NotNull
  public <T> CompletableFuture<List<T>> findBatch(
      final @NotNull Class<T> type,
      final @NotNull Collection<? extends QueryFilter<T>> filters) {
    return this.<T>resolve(type).findBatch(filters);
  }

  @NotNull
  public <T> CompletableFuture<Void> delete(
      final @NotNull Class<T> type,
      final @NotNull QueryFilter<T> filter) {
    return this.<T>resolve(type).delete(filter);
  }

  @NotNull
  public <T> CompletableFuture<List<T>> findAll(final @NotNull Class<T> clazz) {
    return this.<T>resolve(clazz).findAll();
  }

  @NotNull
  public <T> CompletableFuture<List<T>> findAll(
      final @NotNull Class<T> clazz,
      final @NotNull QueryFilter<T> filter) {
    return this.<T>resolve(clazz).findAll(filter);
  }

  @NotNull
  private <T> DatabaseProvider<T> resolve(final @NotNull Class<T> type) {
    final var provider = providers.get(type);

    if (provider != null) {
      return (DatabaseProvider<T>) provider;
    }

    for (final var entry : providers.entrySet()) {
      if (entry.getKey().isAssignableFrom(type)) {
        return (DatabaseProvider<T>) entry.getValue();
      }
    }

    throw new IllegalArgumentException("No provider registered for type: " + type.getName());
  }
}
