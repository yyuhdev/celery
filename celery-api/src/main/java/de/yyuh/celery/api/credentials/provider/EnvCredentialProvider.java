package de.yyuh.celery.api.credentials.provider;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.yyuh.celery.api.IDatabaseType;
import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.credentials.ICredentialProvider;

/**
 * Credential provider that reads credentials from environment variables,
 * with fallback to a {@code .env} file.
 *
 * <p>
 * Resolution order (first wins):
 * <ol>
 * <li>Environment variables ({@code System.getenv})</li>
 * <li>{@code .env} file in the working directory</li>
 * </ol>
 *
 * <p>
 * Keys follow the pattern: {@code {DATABASE_TYPE}_USER},
 * {@code {DATABASE_TYPE}_PASSWORD}, {@code {DATABASE_TYPE}_HOST}, and
 * {@code {DATABASE_TYPE}_PORT}.
 *
 * <p>
 * For example, for MongoDB: {@code MONGODB_USER}, {@code MONGODB_PASSWORD},
 * etc.
 *
 * <p>
 * The .env file is resolved in this order:
 * <ol>
 *   <li>Explicit path via {@code celery.dotenv.path} system property
 *       or {@code CELERY_DOTENV_PATH} environment variable</li>
 *   <li>{@code ./.env} in the working directory</li>
 *   <li>{@code .env} on the classpath (e.g. {@code src/main/resources/.env})</li>
 * </ol>
 *
 * @see SystemPropertyCredentialProvider for the JVM system property equivalent
 */
public final class EnvCredentialProvider implements ICredentialProvider {

  /** {@inheritDoc} */
  @Override
  public @NotNull Optional<Credentials> create(final @NotNull IDatabaseType credentialType) {
    final var prefix = credentialType.name();
    try {
      return Optional.of(new Credentials(
          credOrNull(prefix + "_USER"),
          credOrNull(prefix + "_PASSWORD"),
          credOrDefault(prefix + "_HOST", "localhost"),
          credOrNull(prefix + "_DATABASE"),
          portOrDefault(prefix + "_PORT", credentialType.defaultPort()),
          credOrNull(prefix + "_BUCKET"),
          credOrNull(prefix + "_ACCESS_KEY"),
          credOrNull(prefix + "_ACCESS_KEY_ID"),
          credOrNull(prefix + "_REGION")));
    } catch (final IllegalStateException e) {
      return Optional.empty();
    }
  }

  @Nullable
  private static String credOrNull(final @NotNull String key) {
    final var env = System.getenv(key);
    if (env != null && !env.isBlank()) {
      return env;
    }

    return EnvironmentFileParser.get(key);
  }

  @NotNull
  private static String credOrDefault(final @NotNull String key, final @NotNull String fallback) {
    final var env = System.getenv(key);
    if (env != null && !env.isBlank()) {
      return env;
    }

    return EnvironmentFileParser.getOrDefault(key, fallback);
  }

  private static int portOrDefault(final @NotNull String key, final int fallback) {
    final var env = System.getenv(key);
    if (env != null && !env.isBlank()) {
      return parsePort(key, env);
    }

    final var dotenv = EnvironmentFileParser.get(key);
    if (dotenv != null && !dotenv.isBlank()) {
      return parsePort(key, dotenv);
    }

    return fallback;
  }

  private static int parsePort(final @NotNull String key, final @NotNull String value) {
    try {
      return Integer.parseInt(value);
    } catch (final NumberFormatException e) {
      throw new IllegalStateException("Invalid port in " + key + ": " + value, e);
    }
  }
}
