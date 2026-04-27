package de.yyuh.celery.logging.entity;

import java.time.Instant;

import com.influxdb.annotations.Column;
import de.yyuh.celery.logging.CeleryInfluxDBPlatform;

import de.yyuh.celery.api.PlatformManager;
import de.yyuh.celery.api.entity.ILogEntry;

public abstract class AbstractLogMeasurement implements ILogEntry {

  /**
   * {@link Instant} that automaticly gets created when the
   * {@code AbstractLogMeasurement} is being instantiated
   *
   */
  @Column(timestamp = true)
  private Instant timestamp = Instant.now();

  /**
   * Id of the service that the {@code AbstractLogMeasurement} is being created on
   */
  @Column(tag = true)
  private String serviceId = PlatformManager.getInstance()
      .getPlatform(CeleryInfluxDBPlatform.class)
      .map(platform -> platform.getServiceId())
      .orElse("none");

}
