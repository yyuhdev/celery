package de.yyuh.celery.logging.entity;

import java.time.Instant;

import com.influxdb.annotations.Column;
import de.yyuh.celery.logging.CeleryInfluxDBPlatform;

import de.yyuh.celery.api.PlatformManager;
import de.yyuh.celery.api.entity.ILogEntry;

/**
 * Abstract base class for InfluxDB log measurements.
 *
 * <p>Subclasses represent specific measurement types in InfluxDB.
 * The {@code timestamp} field is automatically set on instantiation
 * and stored as the InfluxDB timestamp. The {@code serviceId} tag
 * identifies which Celery service produced this measurement.
 *
 * <p>Subclasses must be annotated with
 * {@code com.influxdb.annotations.Measurement} and may declare
 * additional {@code @Column} fields.
 */
public abstract class AbstractLogMeasurement implements ILogEntry {

  /**
   * Timestamp automatically set when the measurement is instantiated.
   * Stored as the InfluxDB time column. */
  @Column(timestamp = true)
  private Instant timestamp = Instant.now();

  /**
   * Service ID tag identifying the Celery service that produced this measurement.
   */
  @Column(tag = true)
  private String serviceId = PlatformManager.getInstance()
      .getPlatform(CeleryInfluxDBPlatform.class)
      .map(platform -> platform.getServiceId())
      .orElse("none");

}
