package de.yyuh.celery.api.platform;

import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.IDatabaseType;
import de.yyuh.celery.api.provider.IProvider;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base class for platform implementations.
 *
 * <p>
 * A platform represents a specific database or service type (e.g., MongoDB,
 * Redis)
 * and provides access to its providers.
 */
public abstract class AbstractCeleryPlatform {

  private final String id;
  private final IDatabaseType databaseType;
  private final CeleryPlatformType celeryPlatformType;
  private final Map<Class<? extends IProvider>, IProvider> providers = new HashMap<>();

  /**
   * Creates a new platform with the specified configuration.
   *
   * @param id           the unique identifier for this platform instance
   * @param databaseType the type of database this platform supports
   * @param platformType the platform type category
   */
  public AbstractCeleryPlatform(
      final @NotNull String id,
      final @NotNull IDatabaseType databaseType,
      final @NotNull CeleryPlatformType platformType) {
    this.id = id;
    this.databaseType = databaseType;
    this.celeryPlatformType = platformType;
  }

  /**
   * Returns the unique identifier of this platform.
   *
   * @return the platform ID
   */
  @NotNull
  public String getId() {
    return this.id;
  }

  /**
   * Returns the database type this platform supports.
   *
   * @return the database type
   */
  @NotNull
  public IDatabaseType getDatabaseType() {
    return this.databaseType;
  }

  /**
   * Returns the platform type category.
   *
   * @return the platform type
   */
  @NotNull
  public CeleryPlatformType getCeleryPlatformType() {
    return this.celeryPlatformType;
  }

  /**
   * Returns the default provider for this platform.
   *
   * @return the default provider
   * @deprecated Use {@link #provider(Class)} instead
   */
  @Deprecated
  public abstract IProvider defaultProvider();

  /**
   * Registers a provider for this platform.
   *
   * @param providerClass the class of provider
   * @param provider      the provider instance
   * @param <P>           the provider type
   * @return this platform for chaining
   */
  public <P extends IProvider> AbstractCeleryPlatform registerProvider(
      final @NotNull Class<P> providerClass,
      final @NotNull P provider) {
    this.providers.put(providerClass, provider);
    return this;
  }

  /**
   * Gets a provider by class type.
   *
   * @param providerClass the provider class
   * @param <P>           the provider type
   * @return optional containing the provider if registered
   */
  public <P extends IProvider> Optional<P> provider(final @NotNull Class<P> providerClass) {
    return Optional.ofNullable(providerClass.cast(this.providers.get(providerClass)));
  }
}
