package de.yyuh.celery.api;

import org.jetbrains.annotations.NotNull;

public enum CeleryDatabaseType implements IDatabaseType {

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
      return CeleryPlatformType.STORAGE;
    }
  };

  public abstract int defaultPort();
}
