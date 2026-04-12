package de.yyuh.celery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.yyuh.celery.api.PlatformManager;
import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.credentials.ICredentialProvider;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Celery {

  private static final Logger log = LoggerFactory.getLogger(Celery.class);

  private static Celery instance;

  private final List<ICredentialProvider> credentialProviders = new ArrayList<>();
  private final List<Class<? extends AbstractCeleryPlatform>> platforms = new ArrayList<>();
  private final List<AbstractCeleryPlatform> platformInstances = new ArrayList<>();

  private PlatformManager platformManager;

  private Celery() {
  }

  public static Celery create() {
    return instance = new Celery();
  }

  public static Celery getInstance() {
    return instance;
  }

  @NotNull
  public PlatformManager platformManager() {
    return this.platformManager;
  }

  @NotNull
  @Deprecated
  public Optional<AbstractCeleryPlatform> getPlatformById(final @NotNull String id) {
    return this.platformManager.getPlatform(id);
  }

  @NotNull
  public Celery registerPlatform(final Class<? extends AbstractCeleryPlatform> clazz) {
    this.platforms.add(clazz);
    return this;
  }

  @NotNull
  @SafeVarargs
  public final Celery registerPlatforms(final Class<? extends AbstractCeleryPlatform>... clazz) {
    this.platforms.addAll(Arrays.asList(clazz));
    return this;
  }

  @NotNull
  public Celery registerCredentialProvider(final @NotNull ICredentialProvider provider) {
    this.credentialProviders.add(provider);
    return this;
  }

  @NotNull
  public Celery build() {
    for (final Class<? extends AbstractCeleryPlatform> platformClass : this.platforms) {
      try {
        final AbstractCeleryPlatform platform = platformClass.getDeclaredConstructor().newInstance();
        this.platformInstances.add(platform);

        final Optional<Credentials> credentials = this.resolveCredentials(platform);

        if (credentials.isEmpty()) {
          throw new RuntimeException("Could not resolve credentials for platform: " + platform.getId());
        }

        platform.defaultProvider().connect(credentials.get()).join()
            .ifErr(log::error)
            .ifOk(lg -> {
              log.info(String.format("Connected to %s in %,dms", platform.getId(), lg));
            });

      } catch (Exception exception) {
        throw new RuntimeException("Failed to initialize platform: " + platformClass.getName(), exception);
      }
    }

    this.platformManager = new PlatformManager(this.platformInstances);
    this.platformManager = new PlatformManager(this.platformInstances);

    return this;
  }

  @NotNull
  private Optional<Credentials> resolveCredentials(final @NotNull AbstractCeleryPlatform platform) {
    for (final ICredentialProvider provider : this.credentialProviders) {
      final Optional<Credentials> credentials = provider.create(platform.getDatabaseType());
      if (credentials.isPresent()) {
        return credentials;
      }
    }
    return Optional.empty();
  }
}
