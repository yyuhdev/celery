package de.yyuh.celery.logging;

import de.yyuh.celery.api.CeleryDatabaseType;
import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.IProvider;
import de.yyuh.celery.api.provider.ITimeseriesProvider;
import de.yyuh.celery.logging.provider.InfluxDBTimeseriesProvider;

/**
 * InfluxDB platform implementation for the Celery framework.
 *
 * <p>
 * This platform provides InfluxDB specific logging implementations.
 * Use InfluxDB annotations and APIs directly instead of celery-api
 * abstractions.
 *
 * <p>
 * Example POJO:
 *
 * <pre>
 * {@code
 * import com.influxdb.annotations.Column;
 * import com.influxdb.annotations.Measurement;
 *
 * import de.yyuh.celery.logging.entity.AbstractLogMeasurement;
 *
 * &#64;Measurement(name = "chat_message")
 * public final class ChatMessage extends AbstractLogMeasurement {
 *
 *   &#64;Column
 *   private String message;
 *
 *   &#64;Column
 *   private String sender;
 *
 *   &#64;Column
 *   private boolean flagged;
 *
 *   public ChatMessage(
 *       final String message,
 *       final String sender,
 *       final boolean flagged) {
 *     this.message = message;
 *     this.sender = sender;
 *     this.flagged = flagged;
 *   }
 * }
 * </pre>
 */
public final class CeleryInfluxDBPlatform extends AbstractCeleryPlatform {

  public CeleryInfluxDBPlatform() {
    super("influx", CeleryDatabaseType.INFLUXDB, CeleryPlatformType.TIMESERIES);

    super.registerProvider(ITimeseriesProvider.class, new InfluxDBTimeseriesProvider());
  }

  /** {@inheritDoc} */
  @Override
  public IProvider defaultProvider() {
    return super.provider(ITimeseriesProvider.class).orElseThrow();
  }
}
