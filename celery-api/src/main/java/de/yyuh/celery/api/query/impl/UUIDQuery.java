package de.yyuh.celery.api.query.impl;

import de.yyuh.celery.api.query.AbstractQuery;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public class UUIDQuery<T> extends AbstractQuery<T> {

  protected UUIDQuery(final Builder<T> builder) {
    super(builder);
  }

  @NotNull
  public static <T> Builder<T> builder(final @NotNull Class<T> entityClass, final @NotNull UUID uuid) {
    return new Builder<>(entityClass, uuid);
  }

  public static class Builder<T> extends AbstractQuery.Builder<T, Builder<T>> {
    protected Builder(final @NotNull Class<T> entityClass, final @NotNull UUID uuid) {
      super(entityClass);
      filter("uuid", uuid);
    }

    @Override
    public @NotNull UUIDQuery<T> build() {
      return new UUIDQuery<>(this);
    }
  }
}
