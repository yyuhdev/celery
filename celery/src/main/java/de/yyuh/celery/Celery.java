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
import de.yyuh.celery.api.messaging.MessageRegistry;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.IProvider;
import de.yyuh.celery.api.provider.IReconnectable;
import de.yyuh.libs.core.injection.Injector;
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
  private final MessageRegistry messageRegistry = new MessageRegistry();

  private PlatformManager platformManager;
  private EventBus eventBus;

  private final Injector injector = new Injector();

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
  @Deprecated(forRemoval = true)
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
   * Returns the platform for the given concrete class.
   *
   * @param clazz the platform class to search for
   * @return an Optional containing the platform if found
   */
  @NotNull
  public Optional<AbstractCeleryPlatform> getPlatform(final @NotNull Class<? extends AbstractCeleryPlatform> clazz) {
    return this.platformManager.getPlatform(clazz);
  }

  /**
   * Returns the message bus associated with the given platform or service ID.
   *
   * @param id the message bus ID to search for
   * @return an Optional containing the message bus if found
   */
  @NotNull
  public Optional<MessageBus> getMessageBus(final String id) {
    return Optional.ofNullable(this.messageBusses.get(id));
  }

  /**
   * Registers a platform class for initialization during {@link #build()}.
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
   * Registers multiple platform classes for initialization during
   * {@link #build()}.
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
   * Registers a credential provider for resolving database credentials.
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

            platform.setServiceId(this.id);

            final Optional<Credentials> credentials = this.resolveCredentials(platform);

            if (credentials.isEmpty()) {
              throw new RuntimeException("Could not resolve credentials for platform: " + platform.getId());
            }

            final var result = this.connectPlatform(platform, credentials.get());

            if (result.isErr()) {
              throw new RuntimeException("Could not connect to platform: " + platform.getId());
            }

            this.injectProviderVariables(platform);
          });
    }

    this.platformManager = new PlatformManager(this.platformInstances);
    this.eventBus = new EventBus();

    return this;
  }

  private void injectProviderVariables(final AbstractCeleryPlatform platform) {
    this.injector.bind(EventBus.class, this.eventBus);
    this.injector.bind(MessageBus.class, this.messageBusses.get(platform.getId()));
    this.injector.bind(MessageRegistry.class, this.messageRegistry);

    this.injector.inject(platform.defaultProvider());
  }

  @NotNull
  private Result<IProvider, String> connectPlatform(
      final @NotNull AbstractCeleryPlatform platform,
      final @NotNull Credentials credentials) {
    final var provider = platform.defaultProvider();

    return provider.connect(credentials).join()
        .ifErr(connectException -> {
          log.error("Failed to connect to platform: " + platform.getId(), connectException);
        })
        .ifOk(connectTime -> {
          log.info(String.format("Connected to %s in %,dms", platform.getId(), connectTime));

          if (provider instanceof IReconnectable reconnectable) {
            reconnectable.startAutoReconnect(credentials);
          }

          if (provider instanceof final IMessagingProvider messageHandler) {
            final var messagebus = MessageBus.builder()
                .messagingProvider(messageHandler)
                .serviceId(this.id)
                .build();

            this.messageBusses.put(platform.getId(), messagebus);
          }
        }).map(_ -> provider);
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

  /**
   * Returns the unique identifier for this Celery instance.
   *
   * @return the service ID
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the Protobuf message registry used for type-safe message unpacking.
   *
   * @return the message registry
   */
  public MessageRegistry getMessageRegistry() {
    return messageRegistry;
  }

  /**
   * Returns the platform manager that holds all registered platforms.
   *
   * @return the platform manager
   */
  public PlatformManager getPlatformManager() {
    return platformManager;
  }

  /**
   * Returns the event bus used for local Protobuf event dispatching.
   *
   * @return the event bus
   */
  public EventBus getEventBus() {
    return eventBus;
  }
}
