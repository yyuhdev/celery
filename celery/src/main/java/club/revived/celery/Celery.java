package club.revived.celery;

import java.util.ArrayList;
import java.util.List;

import club.revived.celery.credentials.ICredentialProvider;
import club.revived.celery.credentials.provider.EnvCredentialProvider;

public final class Celery {

  private final List<ICredentialProvider> providers = new ArrayList<>();

  public Celery() {
    this.registerCredentialProviders();
  }

  /**
   * Registers {@link ICredentialProvider}s
   */
  private void registerCredentialProviders() {
    this.providers.add(new EnvCredentialProvider());
  }
}
