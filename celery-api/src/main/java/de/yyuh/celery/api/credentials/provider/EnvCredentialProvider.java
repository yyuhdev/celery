package de.yyuh.celery.api.credentials.provider;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.yyuh.celery.api.IDatabaseType;
import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.credentials.ICredentialProvider;

/**
 * Credential provider that reads credentials from environment variables.
 *
 * <p>
 * Environment variables follow the pattern: {@code {DATABASE_TYPE}_USER},
 * {@code {DATABASE_TYPE}_PASSWORD}, {@code {DATABASE_TYPE}_HOST}, and
 * {@code {DATABASE_TYPE}_PORT}.
 *
 * <p>
 * For example, for MongoDB: {@code MONGODB_USER}, {@code MONGODB_PASSWORD},
 * etc.
 */
public final class EnvCredentialProvider implements ICredentialProvider {

  // TODO: Probably refactor at some point
  /** {@inheritDoc} */
  @Override
  public @NotNull Optional<Credentials> create(final @NotNull IDatabaseType credentialType) {
    final String prefix = credentialType.name();
    try {
      return Optional.of(new Credentials(
          envOrNull(prefix + "_USER"),
          envOrNull(prefix + "_PASSWORD"),
          envOrDefault(prefix + "_HOST", "localhost"),
          envOrNull(prefix + "_DATABASE"),
          portOrDefault(prefix + "_PORT", credentialType.defaultPort()),
          envOrNull(prefix + "_BUCKET"),
          envOrNull(prefix + "_ACCESS_KEY"),
          envOrNull(prefix + "_ACCESS_KEY_ID"),
          envOrNull(prefix + "_REGION")));

    } catch (final IllegalStateException e) {
      return Optional.empty();
    }
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
