package de.yyuh.celery.logging.entity;

import java.time.Instant;

import com.influxdb.annotations.Column;

public abstract class LogEntry {

  @Column(timestamp = true)
  private Instant timestamp = Instant.now();

  @Column(tag = true)
  private String server = "";
}
