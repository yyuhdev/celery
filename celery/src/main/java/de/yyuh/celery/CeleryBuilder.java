package de.yyuh.celery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import de.yyuh.celery.api.credentials.ICredentialProvider;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;

public final class CeleryBuilder {

  private final List<ICredentialProvider> credentialProviders = new ArrayList<>();
  private final List<Class<? extends AbstractCeleryPlatform>> platforms = new ArrayList<>();

  private String id;

  CeleryBuilder() {
  }

  @NotNull
  public CeleryBuilder registerPlatform(final Class<? extends AbstractCeleryPlatform> clazz) {
    this.platforms.add(clazz);
    return this;
  }

  @NotNull
  @SafeVarargs
  public final CeleryBuilder registerPlatforms(final Class<? extends AbstractCeleryPlatform>... clazz) {
    this.platforms.addAll(Arrays.asList(clazz));
    return this;
  }

  @NotNull
  public CeleryBuilder registerCredentialProvider(final @NotNull ICredentialProvider provider) {
    this.credentialProviders.add(provider);
    return this;
  }

  @NotNull
  public CeleryBuilder withId(final String serverId) {
    this.id = serverId;
    return this;
  }

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
