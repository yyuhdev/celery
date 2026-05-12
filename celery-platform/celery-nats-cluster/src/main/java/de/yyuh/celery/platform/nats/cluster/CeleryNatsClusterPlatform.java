package de.yyuh.celery.platform.nats.cluster;

import org.jetbrains.annotations.NotNull;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.platform.nats.cluster.provider.NatsClusterMessagingProvider;

/**
 * NATS Cluster platform for Pub/Sub messaging.
 *
 * <p>
 * This platform uses {@link NatsClusterMessagingProvider} to provide
 * cluster-aware NATS messaging with multi-server support and queue
 * groups for load-balanced message distribution across consumers.
 *
 * <p>
 * The NATS client handles automatic failover — if one server in the
 * cluster goes down, the client transparently reconnects to another
 * available node.
 */
public final class CeleryNatsClusterPlatform extends AbstractCeleryPlatform {

  public CeleryNatsClusterPlatform() {
    super("nats-cluster", CeleryDatabaseType.NATS, CeleryPlatformType.PUBSUB);

    registerProvider(IMessagingProvider.class, new NatsClusterMessagingProvider());
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull IMessagingProvider defaultProvider() {
    return provider(IMessagingProvider.class).orElseThrow();
  }
}
