package de.yyuh.celery.api;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a type of database supported by the Celery framework.
 *
 * <p>
 * Each database type defines its default port and the preferred
 * platform category for storage.
 */
public interface IDatabaseType {

  /**
   * Returns the name of this database type.
   *
   * @return the database type name
   */
  @NotNull
  String name();

  /**
   * Returns the default port for this database type.
   *
   * @return the default port number
   */
  int defaultPort();

  /**
   * Returns the platform type best suited for this database.
   *
   * @return the default platform type
   */
  @NotNull
  CeleryPlatformType defaultPlatform();
}
