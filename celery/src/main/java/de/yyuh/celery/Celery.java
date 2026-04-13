package de.yyuh.celery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.yyuh.celery.api.PlatformManager;
import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.credentials.ICredentialProvider;
import de.yyuh.celery.api.event.EventBus;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Celery framework.
 *
 * <p>
 * Celery provides a fluent API for registering platforms and
 * building the framework with resolved credentials. Use the
 * static factory method {@link #create()} to obtain a new instance.
 */
public final class Celery {

  private static final Logger log = LoggerFactory.getLogger(Celery.class);

  private static Celery instance;

  private final List<ICredentialProvider> credentialProviders = new ArrayList<>();
  private final List<Class<? extends AbstractCeleryPlatform>> platforms = new ArrayList<>();
  private final List<AbstractCeleryPlatform> platformInstances = new ArrayList<>();

  private PlatformManager platformManager;

  private Celery() {
  }

  /**
   * Creates a new Celery instance and sets it as the singleton.
   *
   * @return a new Celery instance
   */
  public static Celery create() {
    return instance = new Celery();
  }

  /**
   * Returns the singleton Celery instance.
   *
   * @return the Celery instance
   */
  public static Celery getInstance() {
    return instance;
  }

  /**
   * Returns the {@link EventBus} for this Celery instance;
   *
   * @return the event bus
   */
  @NotNull
  public EventBus eventBus() {
    return EventBus.instance();
  }

  /**
   * Returns the PlatformManager for this Celery instance.
   *
   * @return the platform manager
   */
  @NotNull
  public PlatformManager platformManager() {
    return this.platformManager;
  }

  /**
   * Finds a platform by its ID.
   *
   * @param id the platform ID
   * @return an Optional containing the platform if found
   * @deprecated Use {@link PlatformManager#getPlatform(String)} instead
   */
  @NotNull
  @Deprecated
  public Optional<AbstractCeleryPlatform> getPlatformById(final @NotNull String id) {
    return this.platformManager.getPlatform(id);
  }

  /**
   * Registers a platform class for initialization.
   *
   * @param clazz the platform class to register
   * @return this Celery instance for method chaining
   */
  @NotNull
  public Celery registerPlatform(final Class<? extends AbstractCeleryPlatform> clazz) {
    this.platforms.add(clazz);
    return this;
  }

  /**
   * Registers multiple platform classes for initialization.
   *
   * @param clazz the platform classes to register
   * @return this Celery instance for method chaining
   */
  @NotNull
  @SafeVarargs
  public final Celery registerPlatforms(final Class<? extends AbstractCeleryPlatform>... clazz) {
    this.platforms.addAll(Arrays.asList(clazz));
    return this;
  }

  /**
   * Registers a credential provider.
   *
   * @param provider the credential provider to register
   * @return this Celery instance for method chaining
   */
  @NotNull
  public Celery registerCredentialProvider(final @NotNull ICredentialProvider provider) {
    this.credentialProviders.add(provider);
    return this;
  }

  /**
   * Builds the Celery framework by initializing all registered platforms.
   *
   * <p>
   * This method instantiates all registered platforms, resolves credentials
   * for each platform, and establishes database connections.
   *
   * @return this Celery instance
   * @throws RuntimeException if platform initialization fails
   */
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
