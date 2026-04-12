package club.revived.celery.credentials;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

public interface ICredentialProvider {

  @NotNull
  Optional<Credentials> create(final DatabaseType credentialType);

}
