package de.yyuh.celery.api;

/**
 * Defines the categories of platform capabilities.
 *
 * <p>
 * Platform types categorize the functionality provided by different
 * database and service implementations.
 */
public enum CeleryPlatformType {

  /** Persistent storage (e.g. MongoDB, MariaDB). */
  STORAGE,
  /** Publish/subscribe messaging (e.g. NATS, Redis). */
  PUBSUB,
  /** Cache storage (e.g. Redis, DragonflyDB). */
  CACHE,
  /** Time-series data storage (e.g. InfluxDB). */
  TIMESERIES,
  /** File/object storage (e.g. Amazon S3). */
  FILE_STORAGE
}
