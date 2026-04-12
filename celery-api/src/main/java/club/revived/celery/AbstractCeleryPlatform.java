package club.revived.celery;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractCeleryPlatform {

  private final String id;

  public AbstractCeleryPlatform(final @NotNull String id) {
    this.id = id;
  }
}
