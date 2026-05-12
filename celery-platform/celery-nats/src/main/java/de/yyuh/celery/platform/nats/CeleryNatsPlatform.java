package de.yyuh.celery.platform.nats;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.IProvider;
import de.yyuh.celery.platform.nats.provider.NatsMessagingProvider;

/**
 * NATS.io platform implementation for the Celery framework.
 *
 * <p>
 * This platform provides NATS-specific messaging operations
 * including Pub/Sub capabilities with the {@code NatsMessagingProvider}.
 *
 */
/**
 * NATS platform implementation for the Celery framework.
 *
 * <p>
 * This platform connects to a single NATS server and provides
 * Pub/Sub messaging via {@link NatsMessagingProvider}.
 * Messages are encoded as Protobuf byte arrays.
 */
public final class CeleryNatsPlatform extends AbstractCeleryPlatform {

  /**
   * Creates a new CeleryNatsPlatform with the NATS messaging provider.
   */
  public CeleryNatsPlatform() {
    super("nats", CeleryDatabaseType.NATS, CeleryPlatformType.PUBSUB);

    super.registerProvider(IMessagingProvider.class, new NatsMessagingProvider());
  }

  /** {@inheritDoc} */
  @Override
  public IMessagingProvider defaultProvider() {
    return provider(IMessagingProvider.class).orElseThrow();
  }
}
