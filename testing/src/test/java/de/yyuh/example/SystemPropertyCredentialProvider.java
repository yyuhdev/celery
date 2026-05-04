package de.yyuh.example;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.credentials.ICredentialProvider;
import de.yyuh.celery.api.IDatabaseType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

// TODO: Refactor and include in celery-api
final class SystemPropertyCredentialProvider implements ICredentialProvider {

  @Override
  public @NotNull Optional<Credentials> create(final @NotNull IDatabaseType type) {
    final String prefix = type.name();
    final String user = System.getProperty(prefix + "_USER");
    final String pass = System.getProperty(prefix + "_PASSWORD");
    final String host = System.getProperty(prefix + "_HOST", "localhost");
    final int port = Integer.parseInt(System.getProperty(prefix + "_PORT", String.valueOf(type.defaultPort())));

    final String bucket = System.getProperty(prefix + "_BUCKET");
    final String accessKey = System.getProperty(prefix + "_ACCESS_KEY");
    final String accessKeyId = System.getProperty(prefix + "_ACCESS_KEY_ID");
    final String region = System.getProperty(prefix + "_REGION");

    if (host == null) {
      return Optional.empty();
    }

    return Optional.of(new Credentials(
        user,
        pass,
        host,
        port,
        bucket,
        accessKey,
        accessKeyId,
        region));
  }
}
