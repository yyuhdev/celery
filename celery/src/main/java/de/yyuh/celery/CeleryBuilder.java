package de.yyuh.celery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import de.yyuh.celery.api.credentials.ICredentialProvider;
import de.yyuh.celery.api.messaging.MessageRegistry;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;

/**
 * Builder for creating Celery instances.
 */
public final class CeleryBuilder {

  private final List<ICredentialProvider> credentialProviders = new ArrayList<>();
  private final List<Class<? extends AbstractCeleryPlatform>> platforms = new ArrayList<>();

  private String id;

  /**
   * Creates a new CeleryBuilder.
   */
  CeleryBuilder() {
  }

  /**
   * Registers a platform class.
   *
   * @param clazz the platform class to register
   * @return this builder for chaining
   */
  @NotNull
  public CeleryBuilder registerPlatform(final Class<? extends AbstractCeleryPlatform> clazz) {
    this.platforms.add(clazz);
    return this;
  }

  /**
   * Registers multiple platform classes.
   *
   * @param clazz the platform classes to register
   * @return this builder for chaining
   */
  @NotNull
  @SafeVarargs
  public final CeleryBuilder registerPlatforms(final Class<? extends AbstractCeleryPlatform>... clazz) {
    this.platforms.addAll(Arrays.asList(clazz));
    return this;
  }

  /**
   * Registers a credential provider.
   *
   * @param provider the credential provider to register
   * @return this builder for chaining
   */
  @NotNull
  public CeleryBuilder registerCredentialProvider(final @NotNull ICredentialProvider provider) {
    this.credentialProviders.add(provider);
    return this;
  }

  /**
   * Sets the service ID.
   *
   * @param serverId the service identifier
   * @return this builder for chaining
   */
  @NotNull
  public CeleryBuilder withId(final String serverId) {
    this.id = serverId;
    return this;
  }

  /**
   * Builds and initializes the Celery instance.
   *
   * @return the built Celery instance
   */
  @NotNull
  public Celery build() {
    final Celery celery = new Celery(id);

    for (final Class<? extends AbstractCeleryPlatform> platform : this.platforms) {
      celery.registerPlatform(platform);
    }

    for (final ICredentialProvider provider : this.credentialProviders) {
      celery.registerCredentialProvider(provider);
    }

    return celery.build();
  }
}
