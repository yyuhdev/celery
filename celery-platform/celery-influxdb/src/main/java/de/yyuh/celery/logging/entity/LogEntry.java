package de.yyuh.celery.logging.entity;

import java.time.Instant;

import com.influxdb.annotations.Column;

/**
 * Abstract base class for simple InfluxDB log entries.
 *
 * <p>Legacy base class that provides a timestamp and server tag.
 * Prefer {@link de.yyuh.celery.logging.entity.AbstractLogMeasurement}
 * for new implementations, as it integrates with the Celery platform
 * and automatically resolves the service ID.
 *
 * @deprecated Use {@link de.yyuh.celery.logging.entity.AbstractLogMeasurement} instead.
 */
@Deprecated
public abstract class LogEntry {

  /** Timestamp automatically set on instantiation. */
  @Column(timestamp = true)
  private Instant timestamp = Instant.now();

  /** Server identifier tag. */
  @Column(tag = true)
  private String server = "";
}
