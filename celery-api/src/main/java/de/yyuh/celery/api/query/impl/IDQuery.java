package de.yyuh.celery.api.query.impl;

import de.yyuh.celery.api.query.AbstractQuery;
import org.jetbrains.annotations.NotNull;

public class IDQuery<T> extends AbstractQuery<T> {

  protected IDQuery(Builder<T> builder) {
    super(builder);
  }

  public static <T> Builder<T> builder(@NotNull Class<T> entityClass, @NotNull Object id) {
    return new Builder<>(entityClass, id);
  }

  public static class Builder<T> extends AbstractQuery.Builder<T, Builder<T>> {
    protected Builder(@NotNull Class<T> entityClass, @NotNull Object id) {
      super(entityClass);
      filter("id", id);
    }

    @Override
    public @NotNull IDQuery<T> build() {
      return new IDQuery<>(this);
    }
  }
}
