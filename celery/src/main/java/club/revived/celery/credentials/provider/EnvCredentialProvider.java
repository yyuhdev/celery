package club.revived.celery.credentials.provider;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import club.revived.celery.credentials.Credentials;
import club.revived.celery.credentials.CredentialType;
import club.revived.celery.credentials.ICredentialProvider;

public final class EnvCredentialProvider implements ICredentialProvider {

  @Override
  public @NotNull Optional<Credentials> create(final @NotNull CredentialType credentialType) {
    final String prefix = credentialType.name();
    try {
      return Optional.of(new Credentials(
          envOrNull(prefix + "_USER"),
          envOrNull(prefix + "_PASSWORD"),
          envOrDefault(prefix + "_HOST", "localhost"),
          portOrDefault(prefix + "_PORT", defaultPort(credentialType))));
    } catch (final IllegalStateException e) {
      return Optional.empty();
    }
  }

  private static int defaultPort(final @NotNull CredentialType type) {
    return switch (type) {
      case MONGODB -> 27017;
      case INFLUXDB -> 8086;
      case REDIS -> 6379;
      case NATS -> 4222;
      case SQL -> 3306;
    };
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
