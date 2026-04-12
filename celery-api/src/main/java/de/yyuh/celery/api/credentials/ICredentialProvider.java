package de.yyuh.celery.api.credentials;

import java.util.Optional;

import de.yyuh.celery.api.IDatabaseType;
import org.jetbrains.annotations.NotNull;

public interface ICredentialProvider {

  @NotNull
  Optional<Credentials> create(final IDatabaseType credentialType);

}
