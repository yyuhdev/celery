package club.revived.celery;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import club.revived.celery.database.model.Entity;
import club.revived.celery.database.model.LogMetric;
import club.revived.celery.database.provider.DatabaseProvider;
import club.revived.celery.database.provider.DatabaseRegistry;
import club.revived.celery.database.provider.impl.InfluxDBProvider;
import club.revived.celery.database.provider.impl.MongoDatabaseProvider;
import club.revived.celery.messaging.provider.PubSubProvider;
import club.revived.celery.messaging.provider.StorageProvider;
import club.revived.celery.messaging.provider.impl.NATSPubSubProvider;
import club.revived.celery.messaging.provider.impl.RedisStorageProvider;

public final class CeleryBuilder {

  @Nullable
  private DatabaseRegistry databaseRegistry;

  @Nullable
  private Credentials mongoCredentials;

  @Nullable
  private Credentials influxCredentials;

  @Nullable
  private PubSubProvider pubSubProvider;

  @Nullable
  private StorageProvider storageProvider;

  @Nullable
  private String nodeId;

  CeleryBuilder() {
  }

  @NotNull
  public CeleryBuilder mongo(final @NotNull Credentials credentials) {
    this.mongoCredentials = credentials;
    return this;
  }

  @NotNull
  public CeleryBuilder mongo() {
    return mongo(CredentialsEnv.mongo());
  }

  @NotNull
  public CeleryBuilder influx(final @NotNull Credentials credentials) {
    this.influxCredentials = credentials;
    return this;
  }

  @NotNull
  public CeleryBuilder influx() {
    return influx(CredentialsEnv.influx());
  }

  @NotNull
  public <T> CeleryBuilder database(
      final @NotNull Class<T> type,
      final @NotNull DatabaseProvider<T> provider,
      final @NotNull Credentials credentials) {
    ensureDatabaseRegistry();
    provider.connect(credentials.toDatabaseCredentials());
    databaseRegistry.register(type, provider, credentials.toDatabaseCredentials());
    return this;
  }

  @NotNull
  public CeleryBuilder nats(final @NotNull Credentials credentials) {
    this.pubSubProvider = new NATSPubSubProvider();
    this.pubSubProvider.connect(credentials.toDatabaseCredentials());
    return this;
  }

  @NotNull
  public CeleryBuilder nats() {
    return nats(CredentialsEnv.nats());
  }

  @NotNull
  public CeleryBuilder pubSub(final @NotNull PubSubProvider provider) {
    this.pubSubProvider = provider;
    return this;
  }

  @NotNull
  public CeleryBuilder redis(final @NotNull Credentials credentials) {
    this.storageProvider = new RedisStorageProvider();
    this.storageProvider.connect(credentials.toDatabaseCredentials());
    return this;
  }

  @NotNull
  public CeleryBuilder redis() {
    return redis(CredentialsEnv.redis());
  }

  @NotNull
  public CeleryBuilder dragonfly() {
    return redis();
  }

  @NotNull
  public CeleryBuilder dragonfly(final @NotNull Credentials credentials) {
    return redis(credentials);
  }

  @NotNull
  public CeleryBuilder storage(final @NotNull StorageProvider provider) {
    this.storageProvider = provider;
    return this;
  }

  @NotNull
  public CeleryBuilder nodeId(final @NotNull String nodeId) {
    this.nodeId = nodeId;
    return this;
  }

  @NotNull
  public Celery build() {
    if (mongoCredentials != null || influxCredentials != null) {
      ensureDatabaseRegistry();

      if (mongoCredentials != null) {
        final var provider = new MongoDatabaseProvider();
        provider.connect(mongoCredentials.toDatabaseCredentials());
        databaseRegistry.register(Entity.class, provider, mongoCredentials.toDatabaseCredentials());
      }

      if (influxCredentials != null) {
        final var provider = new InfluxDBProvider();
        provider.connect(influxCredentials.toDatabaseCredentials());
        databaseRegistry.register(LogMetric.class, provider, influxCredentials.toDatabaseCredentials());
      }
    }

    final String resolvedNodeId;
    if (nodeId != null) {
      resolvedNodeId = nodeId;
    } else {
      final String hostname = System.getenv("HOSTNAME");
      resolvedNodeId = hostname != null ? hostname : "celery-node";
    }

    return new Celery(
        databaseRegistry,
        pubSubProvider,
        storageProvider,
        resolvedNodeId);
  }

  private void ensureDatabaseRegistry() {
    if (databaseRegistry == null) {
      databaseRegistry = new DatabaseRegistry();
    }
  }
}
