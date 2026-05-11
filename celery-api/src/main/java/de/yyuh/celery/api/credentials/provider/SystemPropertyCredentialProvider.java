package de.yyuh.celery.api.credentials.provider;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.yyuh.celery.api.IDatabaseType;
import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.credentials.ICredentialProvider;

/**
 * Credential provider that reads credentials from JVM system properties.
 *
 * <p>
 * System properties follow the pattern: {@code {DATABASE_TYPE}_USER},
 * {@code {DATABASE_TYPE}_PASSWORD}, {@code {DATABASE_TYPE}_HOST}, and
 * {@code {DATABASE_TYPE}_PORT}.
 *
 * <p>
 * Example: {@code java -DMONGODB_USER=admin -DMONGODB_PASSWORD=secret ...}
 *
 * @see EnvCredentialProvider for the environment variable equivalent
 */
public final class SystemPropertyCredentialProvider implements ICredentialProvider {

  /** {@inheritDoc} */
  @Override
  public @NotNull Optional<Credentials> create(final @NotNull IDatabaseType credentialType) {
    final var prefix = credentialType.name();
    try {
      return Optional.of(new Credentials(
          propOrNull(prefix + "_USER"),
          propOrNull(prefix + "_PASSWORD"),
          propOrDefault(prefix + "_HOST", "localhost"),
          propOrNull(prefix + "_DATABASE"),
          portOrDefault(prefix + "_PORT", credentialType.defaultPort()),
          propOrNull(prefix + "_BUCKET"),
          propOrNull(prefix + "_ACCESS_KEY"),
          propOrNull(prefix + "_ACCESS_KEY_ID"),
          propOrNull(prefix + "_REGION")));
    } catch (final IllegalStateException e) {
      return Optional.empty();
    }
  }

  @Nullable
  private static String propOrNull(final @NotNull String key) {
    final var value = System.getProperty(key);
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }

  @NotNull
  private static String propOrDefault(final @NotNull String key, final @NotNull String fallback) {
    final var value = System.getProperty(key);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value;
  }

  private static int portOrDefault(final @NotNull String key, final int fallback) {
    final var value = System.getProperty(key);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return Integer.parseInt(value);
    } catch (final NumberFormatException e) {
      throw new IllegalStateException("Invalid port in system property " + key + ": " + value, e);
    }
  }
}
