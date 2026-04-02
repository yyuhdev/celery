package club.revived.celery.messaging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import club.revived.celery.database.model.DatabaseCredentials;

public final class DatabaseCredentialsEnv {

  private DatabaseCredentialsEnv() {
  }

  @NotNull
  public static DatabaseCredentials nats() {
    return new DatabaseCredentials(
        null,
        envOrDefault("NATS_HOST", "localhost"),
        envOrNull("NATS_PASSWORD"),
        portOrDefault("NATS_PORT", 4222),
        envOrNull("NATS_DATABASE"));
  }

  @NotNull
  public static DatabaseCredentials redis() {
    return new DatabaseCredentials(
        null,
        envOrDefault("REDIS_HOST", "localhost"),
        envOrNull("REDIS_PASSWORD"),
        portOrDefault("REDIS_PORT", 6379),
        envOrNull("REDIS_DATABASE"));
  }

  @NotNull
  private static String envOrDefault(final @NotNull String key, final @NotNull String fallback) {
    final var value = System.getenv(key);
    if (value == null || value.isBlank()) {
      return fallback;
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
