package de.yyuh.celery.api.credentials.provider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight .env file parser.
 *
 * <p>
 * Parses KEY=VALUE pairs with support for:
 * <ul>
 * <li>Lines starting with # as comments</li>
 * <li>Double- and single-quoted values</li>
 * <li>Inline comments after unquoted values</li>
 * <li>Empty lines</li>
 * </ul>
 *
 * <p>
 * Loaded once and cached. Use {@link #reload()} to force re-read.
 */
final class EnvironmentFileParser {

  private static final Logger log = LoggerFactory.getLogger(EnvironmentFileParser.class);

  private static volatile Map<String, String> entries = Collections.emptyMap();
  private static final AtomicBoolean loaded = new AtomicBoolean(false);

  private EnvironmentFileParser() {
  }

  @NotNull
  static Map<String, String> load() {
    if (loaded.get()) {
      return entries;
    }

    final var envFilePath = resolvePath();
    if (envFilePath == null) {
      loaded.set(true);
      return entries;
    }

    try {
      entries = parse(envFilePath);
      log.debug("Loaded {} entries from {}", entries.size(), envFilePath);
    } catch (final IOException e) {
      log.warn("Failed to read .env file at {}: {}", envFilePath, e.getMessage());
    }

    loaded.set(true);
    return entries;
  }

  static void reload() {
    loaded.set(false);
    entries = Collections.emptyMap();
    load();
  }

  @Nullable
  static String get(final @NotNull String key) {
    load();
    return entries.get(key);
  }

  @NotNull
  static String getOrDefault(final @NotNull String key, final @NotNull String fallback) {
    load();
    return entries.getOrDefault(key, fallback);
  }

  @NotNull
  private static Map<String, String> parse(final @NotNull Path path) throws IOException {
    final var result = new LinkedHashMap<String, String>();

    for (final String line : Files.readAllLines(path)) {
      final var trimmed = line.strip();

      if (trimmed.isEmpty() || trimmed.startsWith("#")) {
        continue;
      }

      final var eqIdx = trimmed.indexOf('=');

      if (eqIdx < 0) {
        continue;
      }

      final var key = trimmed.substring(0, eqIdx).strip();
      final var rawValue = trimmed.substring(eqIdx + 1).strip();

      result.put(key, unquote(stripInlineComment(rawValue)));
    }

    return Collections.unmodifiableMap(result);
  }

  @NotNull
  private static String stripInlineComment(final @NotNull String value) {
    if (value.startsWith("\"") || value.startsWith("'")) {
      return value;
    }

    final var hashIdx = value.indexOf('#');
    if (hashIdx >= 0) {
      return value.substring(0, hashIdx).stripTrailing();
    }

    return value;
  }

  @NotNull
  private static String unquote(final @NotNull String value) {
    if (value.length() >= 2) {
      final var first = value.charAt(0);
      final var last = value.charAt(value.length() - 1);

      if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
        return value.substring(1, value.length() - 1);
      }
    }

    return value;
  }

  @Nullable
  private static Path resolvePath() {
    final var explicit = System.getProperty("celery.dotenv.path");
    if (explicit != null && !explicit.isBlank()) {
      return Path.of(explicit);
    }

    final var envExplicit = System.getenv("CELERY_DOTENV_PATH");
    if (envExplicit != null && !envExplicit.isBlank()) {
      return Path.of(envExplicit);
    }

    final var cwd = Path.of("").toAbsolutePath().resolve(".env");
    if (Files.exists(cwd)) {
      return cwd;
    }

    return null;
  }
}
