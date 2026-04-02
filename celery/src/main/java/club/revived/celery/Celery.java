package club.revived.celery;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

import club.revived.celery.database.model.query.QueryFilter;
import club.revived.celery.database.provider.DatabaseRegistry;
import club.revived.celery.messaging.messaging.MessageManager;
import club.revived.celery.messaging.provider.PubSubProvider;
import club.revived.celery.messaging.provider.StorageProvider;
import club.revived.celery.messaging.storage.CacheManager;
import club.revived.celery.messaging.storage.Watcher;

public final class Celery {

  private static Celery instance;

  @Nullable
  private final DatabaseRegistry databaseRegistry;

  @Nullable
  private final PubSubProvider pubSubProvider;

  @Nullable
  private final StorageProvider storageProvider;

  @NotNull
  private final String nodeId;

  @NotNull
  private final ScheduledExecutorService scheduler;

  @NotNull
  private final MessageManager messageManager;

  @NotNull
  private final CacheManager cacheManager;

  @NotNull
  private final Watcher watcher;

  @NotNull
  private final Gson gson;

  Celery(
      final @Nullable DatabaseRegistry databaseRegistry,
      final @Nullable PubSubProvider pubSubProvider,
      final @Nullable StorageProvider storageProvider,
      final @NotNull String nodeId) {
    this.databaseRegistry = databaseRegistry;
    this.pubSubProvider = pubSubProvider;
    this.storageProvider = storageProvider;
    this.nodeId = nodeId;
    this.scheduler = Executors.newSingleThreadScheduledExecutor();
    this.messageManager = new MessageManager();
    this.cacheManager = new CacheManager();
    this.watcher = new Watcher();
    this.gson = new GsonBuilder()
        .setLongSerializationPolicy(LongSerializationPolicy.STRING)
        .create();

    instance = this;
  }

  @NotNull
  public static CeleryBuilder builder() {
    if (instance != null) {
      throw new IllegalStateException("Celery instance already exists! Use Celery.instance() to access it.");
    }
    return new CeleryBuilder();
  }

  @NotNull
  public static Celery instance() {
    if (instance == null) {
      throw new IllegalStateException("Celery has not been initialized. Call Celery.builder().build() first.");
    }
    return instance;
  }

  public static void reset() {
    if (instance != null) {
      instance.shutdown();
    }
    instance = null;
  }

  @NotNull
  public <T> CompletableFuture<Void> write(
      final @NotNull Class<T> type,
      final @NotNull T entity) {
    this.requireDatabase();
    return databaseRegistry.write(type, entity);
  }

  @NotNull
  public <T> CompletableFuture<Void> writeBatch(
      final @NotNull Class<T> type,
      final @NotNull List<T> entities) {
    this.requireDatabase();
    return databaseRegistry.writeBatch(type, entities);
  }

  @NotNull
  public <T> CompletableFuture<Void> delete(
      final @NotNull Class<T> type,
      final @NotNull QueryFilter<T> filter) {
    this.requireDatabase();
    return databaseRegistry.delete(type, filter);
  }

  @NotNull
  public <T> CompletableFuture<List<T>> findAll(final @NotNull Class<T> type) {
    this.requireDatabase();
    return databaseRegistry.findAll(type);
  }

  @NotNull
  public <T> CompletableFuture<List<T>> findAll(
      final @NotNull Class<T> type,
      final @NotNull QueryFilter<T> filter) {
    this.requireDatabase();
    return databaseRegistry.findAll(type, filter);
  }

  @NotNull
  public <T> CompletableFuture<Optional<T>> find(
      final @NotNull Class<T> type,
      final @NotNull QueryFilter<T> filter) {
    this.requireDatabase();
    return databaseRegistry.find(type, filter);
  }

  @NotNull
  public <T> CompletableFuture<List<T>> findBatch(
      final @NotNull Class<T> type,
      final @NotNull Collection<? extends QueryFilter<T>> filters) {
    this.requireDatabase();
    return databaseRegistry.findBatch(type, filters);
  }

  @NotNull
  public CompletableFuture<Void> publish(
      final @NotNull String channel,
      final byte @NotNull [] message) {
    requirePubSub();
    return pubSubProvider.publish(channel, message);
  }

  public void subscribe(final @NotNull String channel) {
    requirePubSub();
    pubSubProvider.subscribe(channel, data -> messageManager.handleIncoming(channel, data));
  }

  @Nullable
  public DatabaseRegistry database() {
    return databaseRegistry;
  }

  @NotNull
  public PubSubProvider pubSub() {
    requirePubSub();
    return pubSubProvider;
  }

  @NotNull
  public StorageProvider storage() {
    requireStorage();
    return storageProvider;
  }

  @NotNull
  public String nodeId() {
    return nodeId;
  }

  @NotNull
  public ScheduledExecutorService scheduler() {
    return scheduler;
  }

  @NotNull
  public MessageManager messages() {
    return messageManager;
  }

  @NotNull
  public CacheManager cache() {
    return cacheManager;
  }

  @NotNull
  public Watcher watcher() {
    return watcher;
  }

  @NotNull
  public Gson gson() {
    return gson;
  }

  public void shutdown() {
    scheduler.shutdown();
  }

  private void requireDatabase() {
    if (databaseRegistry == null) {
      throw new IllegalStateException("Database not configured. Use CeleryBuilder.mongo() or CeleryBuilder.influx().");
    }
  }

  private void requirePubSub() {
    if (pubSubProvider == null) {
      throw new IllegalStateException("PubSub not configured. Use CeleryBuilder.nats().");
    }
  }

  private void requireStorage() {
    if (storageProvider == null) {
      throw new IllegalStateException("Storage not configured. Use CeleryBuilder.redis().");
    }
  }

  @Deprecated
  @NotNull
  public MessageManager messageManager() {
    return messageManager;
  }

  @Deprecated
  @NotNull
  public CacheManager cacheManager() {
    return cacheManager;
  }

  @Deprecated
  @NotNull
  public PubSubProvider pubSubProvider() {
    requirePubSub();
    return pubSubProvider;
  }

  @Deprecated
  @NotNull
  public StorageProvider storageProvider() {
    requireStorage();
    return storageProvider;
  }
}
