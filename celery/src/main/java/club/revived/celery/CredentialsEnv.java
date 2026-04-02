package club.revived.celery;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CredentialsEnv {

  private CredentialsEnv() {
  }

  @NotNull
  public static Credentials mongo() {
    return Credentials.forMongo(
        env("MONGO_USER"),
        env("MONGO_HOST"),
        envOrNull("MONGO_PASSWORD"),
        portOrDefault("MONGO_PORT", 27017),
        env("MONGO_DATABASE"));
  }

  @NotNull
  public static Credentials influx() {
    return Credentials.forInflux(
        env("INFLUX_ORG"),
        env("INFLUX_HOST"),
        env("INFLUX_TOKEN"),
        portOrDefault("INFLUX_PORT", 8086),
        env("INFLUX_BUCKET"));
  }

  @NotNull
  public static Credentials redis() {
    return Credentials.forRedis(
        envOrDefault("REDIS_HOST", "localhost"),
        envOrNull("REDIS_PASSWORD"),
        portOrDefault("REDIS_PORT", 6379),
        envOrNull("REDIS_DATABASE"));
  }

  @NotNull
  public static Credentials dragonfly() {
    return redis();
  }

  @NotNull
  public static Credentials nats() {
    return Credentials.forNats(
        envOrDefault("NATS_HOST", "localhost"),
        envOrNull("NATS_PASSWORD"),
        portOrDefault("NATS_PORT", 4222),
        envOrNull("NATS_DATABASE"));
  }

  @NotNull
  private static String env(final @NotNull String key) {
    final var value = System.getenv(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing required environment variable: " + key);
    }
    return value;
  }

  @Nullable
  private static String envOrNull(final @NotNull String key) {
    final var value = System.getenv(key);
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }

  @NotNull
  private static String envOrDefault(final @NotNull String key, final @NotNull String fallback) {
    final var value = System.getenv(key);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value;
  }

  private static int portOrDefault(final @NotNull String key, final int fallback) {
    final var value = System.getenv(key);
    if (value == null || value.isBlank()) {
      return fallback;
    }

    try {
      return Integer.parseInt(value);
    } catch (final NumberFormatException e) {
      throw new IllegalStateException("Invalid port in environment variable " + key + ": " + value, e);
    }
  }
}
