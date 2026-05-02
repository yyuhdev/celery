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

  S3 {
    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.FILE_STORAGE;
    }

    @Override
    public int defaultPort() {
      return 80;
    }
  },

  DRAGONFLYDB {
    @Override
    public int defaultPort() {
      return 6379;
    }

    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.CACHE;
    }
  },

  REDIS {
    @Override
    public int defaultPort() {
      return 6379;
    }

    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.PUBSUB;
    }
  },

  MONGODB {
    @Override
    public int defaultPort() {
      return 27017;
    }

    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.STORAGE;
    }
  },

  MARIADB {
    @Override
    public int defaultPort() {
      return 3306;
    }

    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.STORAGE;
    }
  },

  NATS {
    @Override
    public int defaultPort() {
      return 4222;
    }

    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.PUBSUB;
    }
  },

  INFLUXDB {
    @Override
    public int defaultPort() {
      return 8086;
    }

    @Override
    public @NotNull CeleryPlatformType defaultPlatform() {
      return CeleryPlatformType.TIMESERIES;
    }
  };

  public abstract int defaultPort();

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public @NotNull CeleryPlatformType defaultPlatform() {
    return null;
  }
}
