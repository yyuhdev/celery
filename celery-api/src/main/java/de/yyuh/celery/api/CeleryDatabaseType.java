package de.yyuh.celery.api;

import org.jetbrains.annotations.NotNull;

/**
 * Enum implementing IDatabaseType for all supported databases.
 *
 * <p>
 * Each database type defines its default port and the platform
 * category it best serves (storage, pubsub, or cache).
 */
public enum CeleryDatabaseType implements IDatabaseType {

  /** Amazon S3 object storage. Defaults to FILE_STORAGE platform. */
  S3 {
    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.FILE_STORAGE;
    }

    /** {@inheritDoc} */
    @Override
    public int defaultPort() {
      return 80;
    }
  },

  /** DragonflyDB, a drop-in Redis replacement. Defaults to CACHE platform. */
  DRAGONFLYDB {
    @Override
    public int defaultPort() {
      return 6379;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.CACHE;
    }
  },

  /** Redis key-value store. Defaults to PUBSUB platform. */
  REDIS {
    @Override
    public int defaultPort() {
      return 6379;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.PUBSUB;
    }
  },

  /** MongoDB document database. Defaults to STORAGE platform. */
  MONGODB {
    @Override
    public int defaultPort() {
      return 27017;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.STORAGE;
    }
  },

  /** MariaDB relational database. Defaults to STORAGE platform. */
  MARIADB {
    @Override
    public int defaultPort() {
      return 3306;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.STORAGE;
    }
  },

  /** NATS messaging system. Defaults to PUBSUB platform. */
  NATS {
    @Override
    public int defaultPort() {
      return 4222;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.PUBSUB;
    }
  },

  /** InfluxDB time-series database. Defaults to TIMESERIES platform. */
  INFLUXDB {
    @Override
    public int defaultPort() {
      return 8086;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.TIMESERIES;
    }
  };

  /**
   * Returns the default port for this database type.
   *
   * @return the default port number
   */
  public abstract int defaultPort();

  @Override
  public String toString() {
    return super.toString();
  }

  /**
   * Returns the platform type best suited for this database.
   *
   * <p>Each enum constant overrides this to return the appropriate
   * {@link CeleryPlatformType} for its database.
   *
   * @return the default platform type
   */
  @Override
  public @NotNull CeleryPlatformType defaultPlatform() {
    return null;
  }
}
