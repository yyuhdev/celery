package club.revived.celery.database;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import club.revived.celery.database.model.query.QueryFilter;
import club.revived.celery.database.provider.DatabaseRegistry;

public final class Compositio {

  @Nullable
  private static Compositio instance;

  @NotNull
  private final DatabaseRegistry registry;

  Compositio(final @NotNull DatabaseRegistry registry) {
    this.registry = registry;
    instance = this;
  }

  @NotNull
  public static CompositioBuilder builder() {
    if (instance != null) {
      throw new IllegalStateException("Compositio instance already exists!");
    }
    return new CompositioBuilder();
  }

  @NotNull
  public static Compositio instance() {
    if (instance == null) {
      throw new IllegalStateException("Compositio instance has not been initialized yet!");
    }
    return instance;
  }

  @NotNull
  public <T> CompletableFuture<Void> write(
      final @NotNull Class<T> type,
      final @NotNull T entity) {
    return registry.write(type, entity);
  }

  @NotNull
  public <T> CompletableFuture<Void> writeBatch(
      final @NotNull Class<T> type,
      final @NotNull List<T> entities) {
    return registry.writeBatch(type, entities);
  }

  @NotNull
  public <T> CompletableFuture<Void> delete(
      final @NotNull Class<T> clazz,
      final @NotNull QueryFilter<T> filter) {
    return registry.delete(clazz, filter);
  }

  @NotNull
  public <T> CompletableFuture<List<T>> findAll(final @NotNull Class<T> clazz) {
    return registry.findAll(clazz);
  }

  @NotNull
  public <T> CompletableFuture<List<T>> findAll(
      final @NotNull Class<T> clazz,
      final @NotNull QueryFilter<T> filter) {
    return registry.findAll(clazz, filter);
  }

  @NotNull
  public <T> CompletableFuture<Optional<T>> find(
      final @NotNull Class<T> type,
      final @NotNull QueryFilter<T> filter) {
    return registry.find(type, filter);
  }

  @NotNull
  public <T> CompletableFuture<List<T>> findBatch(
      final @NotNull Class<T> type,
      final @NotNull Collection<? extends QueryFilter<T>> filters) {
    return registry.findBatch(type, filters);
  }
}
