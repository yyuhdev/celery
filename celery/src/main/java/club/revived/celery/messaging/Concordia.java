package club.revived.celery.messaging;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

import club.revived.celery.messaging.messaging.MessageManager;
import club.revived.celery.messaging.provider.PubSubProvider;
import club.revived.celery.messaging.provider.StorageProvider;
import club.revived.celery.messaging.storage.CacheManager;
import club.revived.celery.messaging.storage.Watcher;

public final class Concordia {

  private static Concordia instance;

  @NotNull
  private final PubSubProvider pubSubProvider;

  @NotNull
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

  private Concordia(
      final @NotNull PubSubProvider pubSubProvider,
      final @NotNull StorageProvider storageProvider,
      final @NotNull String nodeId) {
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
  }

  public static void init(
      final @NotNull PubSubProvider pubSubProvider,
      final @NotNull StorageProvider storageProvider,
      final @NotNull String nodeId) {
    if (instance != null) {
      throw new IllegalStateException("Concordia is already initialized");
    }
    instance = new Concordia(pubSubProvider, storageProvider, nodeId);
  }

  @NotNull
  public static Concordia instance() {
    if (instance == null) {
      throw new IllegalStateException("Concordia is not initialized");
    }
    return instance;
  }

  @NotNull
  public PubSubProvider pubSubProvider() {
    return pubSubProvider;
  }

  @NotNull
  public StorageProvider storageProvider() {
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
  public MessageManager messageManager() {
    return messageManager;
  }

  @NotNull
  public CacheManager cacheManager() {
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

  @NotNull
  public static ConcordiaBuilder builder() {
    return new ConcordiaBuilder();
  }

  public void subscribe(final @NotNull String channel) {
    pubSubProvider.subscribe(channel, data -> messageManager.handleIncoming(channel, data));
  }

  public void shutdown() {
    scheduler.shutdown();
  }
}
