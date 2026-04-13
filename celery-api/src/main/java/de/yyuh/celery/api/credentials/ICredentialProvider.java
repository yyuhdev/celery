package de.yyuh.celery.api.credentials;

import java.util.Optional;

import de.yyuh.celery.api.IDatabaseType;
import org.jetbrains.annotations.NotNull;

/**
 * Provides database credentials for a specific database type.
 *
 * <p>Credential providers are used to supply authentication
 * information for database connections.
 */
public interface ICredentialProvider {

  /**
   * Creates credentials for the specified database type.
   *
   * @param credentialType the type of database to create credentials for
   * @return an Optional containing the credentials if available
   */
  @NotNull
  Optional<Credentials> create(final IDatabaseType credentialType);

}
