package club.revived.celery.messaging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import club.revived.celery.messaging.provider.PubSubProvider;
import club.revived.celery.messaging.provider.StorageProvider;
import club.revived.celery.messaging.provider.impl.NATSPubSubProvider;
import club.revived.celery.messaging.provider.impl.RedisStorageProvider;

public final class ConcordiaBuilder {

  @Nullable
  private PubSubProvider pubSubProvider;

  @Nullable
  private StorageProvider storageProvider;

  ConcordiaBuilder() {
  }

  @NotNull
  public ConcordiaBuilder nats() {
    this.pubSubProvider = new NATSPubSubProvider();
    this.pubSubProvider.connect(DatabaseCredentialsEnv.nats());
    return this;
  }

  @NotNull
  public ConcordiaBuilder dragonfly() {
    this.storageProvider = new RedisStorageProvider();
    this.storageProvider.connect(DatabaseCredentialsEnv.redis());
    return this;
  }

  @NotNull
  public Concordia build() {
    final var hostName = System.getenv("HOSTNAME");

    Concordia.init(
        pubSubProvider,
        storageProvider,
        hostName);

    return Concordia.instance();
  }
}
