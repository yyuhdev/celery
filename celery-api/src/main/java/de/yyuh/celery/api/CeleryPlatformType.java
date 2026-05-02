package de.yyuh.celery.api;

/**
 * Defines the categories of platform capabilities.
 *
 * <p>
 * Platform types categorize the functionality provided by different
 * database and service implementations.
 */
public enum CeleryPlatformType {

  STORAGE,
  PUBSUB,
  CACHE,
  TIMESERIES,
  FILE_STORAGE
}
