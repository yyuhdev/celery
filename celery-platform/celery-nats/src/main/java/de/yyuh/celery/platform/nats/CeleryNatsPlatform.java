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
public final class CeleryNatsPlatform extends AbstractCeleryPlatform {

  public CeleryNatsPlatform() {
    super("nats", CeleryDatabaseType.NATS, CeleryPlatformType.PUBSUB);

    super.registerProvider(IMessagingProvider.class, new NatsMessagingProvider());
  }

  @Override
  public IProvider defaultProvider() {
    return new NatsMessagingProvider();
  }
}
