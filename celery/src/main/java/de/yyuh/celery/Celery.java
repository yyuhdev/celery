package de.yyuh.celery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.yyuh.celery.api.PlatformManager;
import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.credentials.ICredentialProvider;
import de.yyuh.celery.api.event.EventBus;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.celery.api.messaging.MessageBus;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.libs.core.result.Result;

/**
 * Main entry point for the Celery framework.
 *
 * <p>
 * Celery provides a fluent API for registering platforms and
 * building the framework with resolved credentials. Use the
 * static factory method {@link #builder()} to obtain a new builder instance.
 */
public final class Celery {

  private static final Logger log = LoggerFactory.getLogger(Celery.class);

  private static Celery instance;

  private final List<ICredentialProvider> credentialProviders = new ArrayList<>();
  private final List<Class<? extends AbstractCeleryPlatform>> platforms = new ArrayList<>();
  private final List<AbstractCeleryPlatform> platformInstances = new ArrayList<>();

  private final Map<String, MessageBus> messageBusses = new ConcurrentHashMap<>();

  private final String id;

  private PlatformManager platformManager;

  /**
   * Creates a new Celery instance with the specified ID.
   *
   * @param id the unique identifier for this Celery instance
   */
  Celery(final String id) {
    this.id = id;
  }

  /**
   * Creates a new Celery builder.
   *
   * @return a new CeleryBuilder instance
   */
  @NotNull
  public static CeleryBuilder builder() {
    return new CeleryBuilder();
  }

  /**
   * Creates a new Celery instance and sets it as the singleton.
   *
   * @return a new Celery instance
   * @deprecated Use {@link #builder()} instead
   */
  @Deprecated
  public static Celery create() {
    return instance = new Celery("null");
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
  /**
   * Returns the platform with the specified ID.
   *
   * @param id the platform ID to search for
   * @return an Optional containing the platform if found
   */
  @NotNull
  public Optional<AbstractCeleryPlatform> getPlatformById(final @NotNull String id) {
    return this.platformManager.getPlatform(id);
  }

  /**
   * Returns the message bus with the specified ID.
   *
   * @param id the message bus ID to search for
   * @return an Optional containing the message bus if found
   */
  @NotNull
  public Optional<MessageBus> getMessageBus(final String id) {
    return Optional.ofNullable(this.messageBusses.get(id));
  }

  /**
   * Registers a platform class for initialization.
   *
   * @param clazz the platform class to register
   * @return this Celery instance for chaining
   */
  @NotNull
  Celery registerPlatform(final Class<? extends AbstractCeleryPlatform> clazz) {
    this.platforms.add(clazz);
    return this;
  }

  /**
   * Registers multiple platform classes for initialization.
   *
   * @param clazz the platform classes to register
   * @return this Celery instance for chaining
   */
  @SafeVarargs
  final Celery registerPlatforms(final Class<? extends AbstractCeleryPlatform>... clazz) {
    this.platforms.addAll(Arrays.asList(clazz));
    return this;
  }

  /**
   * Registers a credential provider for resolving credentials.
   *
   * @param provider the credential provider to register
   * @return this Celery instance for chaining
   */
  @NotNull
  Celery registerCredentialProvider(final @NotNull ICredentialProvider provider) {
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
      Result.of(() -> platformClass.getDeclaredConstructor().newInstance())
          .ifErr(exception -> {
            throw new RuntimeException("Failed to initialize platform: " + platformClass.getName(), exception);
          })
          .ifOk(platform -> {
            this.platformInstances.add(platform);

            final Optional<Credentials> credentials = this.resolveCredentials(platform);

            if (credentials.isEmpty()) {
              throw new RuntimeException("Could not resolve credentials for platform: " + platform.getId());
            }

            final var provider = platform.defaultProvider();

            provider.connect(credentials.get()).join()
                .ifErr(connectException -> {
                  log.error("Failed to connect to platform: " + platform.getId(), connectException);
                })
                .ifOk(connectTime -> {
                  log.info(String.format("Connected to %s in %,dms", platform.getId(), connectTime));

                  if (provider instanceof final IMessagingProvider messageHandler) {
                    final var messagebus = MessageBus.builder()
                        .messagingProvider(messageHandler)
                        .serviceId(this.id)
                        .build();

                    this.messageBusses.put(platform.getId(), messagebus);
                  }
                });
          });
    }

    this.platformManager = new PlatformManager(this.platformInstances);

    return this;
  }

  /**
   * Resolves credentials for a platform using registered credential providers.
   *
   * @param platform the platform to resolve credentials for
   * @return an Optional containing the resolved credentials
   */
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
