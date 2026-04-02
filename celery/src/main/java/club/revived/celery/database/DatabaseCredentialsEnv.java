package club.revived.celery.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import club.revived.celery.database.model.DatabaseCredentials;

public final class DatabaseCredentialsEnv {

  private DatabaseCredentialsEnv() {
  }

  @NotNull
  public static DatabaseCredentials mongo() {
    return new DatabaseCredentials(
        env("MONGO_USER"),
        env("MONGO_HOST"),
        envOrNull("MONGO_PASSWORD"),
        portOrDefault("MONGO_PORT", 27017),
        env("MONGO_DATABASE"));
  }

  @NotNull
  public static DatabaseCredentials influx() {
    return new DatabaseCredentials(
        env("INFLUX_ORG"),
        env("INFLUX_HOST"),
        env("INFLUX_TOKEN"),
        portOrDefault("INFLUX_PORT", 8086),
        env("INFLUX_BUCKET"));
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
