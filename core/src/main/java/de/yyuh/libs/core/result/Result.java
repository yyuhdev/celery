package de.yyuh.libs.core.result;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

public sealed interface Result<T, E> permits Result.Ok, Result.Err {

  static <T, E> @NotNull Result<T, E> ok(final @NotNull T value) {
    return new Ok<>(value);
  }

  static <T, E> @NotNull Result<T, E> err(final @NotNull E error) {
    return new Err<>(error);
  }

  boolean isOk();

  default boolean isErr() {
    return !isOk();
  }

  @NotNull
  T unwrap();

  @NotNull
  T unwrapOr(final @NotNull T defaultValue);

  @NotNull
  T unwrapOrElse(final @NotNull Function<E, T> fn);

  @NotNull
  E unwrapErr();

  <U> @NotNull Result<U, E> map(final @NotNull Function<T, U> fn);

  <F> @NotNull Result<T, F> mapErr(final @NotNull Function<E, F> fn);

  <U> @NotNull Result<U, E> flatMap(final @NotNull Function<T, Result<U, E>> fn);

  @NotNull
  Result<T, E> ifOk(final @NotNull Consumer<T> action);

  @NotNull
  Result<T, E> ifErr(final @NotNull Consumer<E> action);

  @NotNull
  Optional<T> ok();

  @NotNull
  Optional<E> err();

  record Ok<T, E>(@NotNull T value) implements Result<T, E> {

    public Ok {
      if (value == null)
        throw new IllegalArgumentException("Ok value must not be null");
    }

    @Override
    public boolean isOk() {
      return true;
    }

    @Override
    public @NotNull T unwrap() {
      return value;
    }

    @Override
    public @NotNull T unwrapOr(final @NotNull T defaultValue) {
      return value;
    }

    @Override
    public @NotNull T unwrapOrElse(final @NotNull Function<E, T> fn) {
      return value;
    }

    @Override
    public @NotNull E unwrapErr() {
      throw new NoSuchElementException("Called unwrapErr() on Ok: " + value);
    }

    @Override
    public <U> @NotNull Result<U, E> map(final @NotNull Function<T, U> fn) {
      return Result.ok(fn.apply(value));
    }

    @Override
    public <F> @NotNull Result<T, F> mapErr(final @NotNull Function<E, F> fn) {
      return Result.ok(value);
    }

    @Override
    public <U> @NotNull Result<U, E> flatMap(final @NotNull Function<T, Result<U, E>> fn) {
      return fn.apply(value);
    }

    @Override
    public @NotNull Result<T, E> ifOk(final @NotNull Consumer<T> action) {
      action.accept(value);
      return this;
    }

    @Override
    public @NotNull Result<T, E> ifErr(final @NotNull Consumer<E> action) {
      return this;
    }

    @Override
    public @NotNull Optional<T> ok() {
      return Optional.of(value);
    }

    @Override
    public @NotNull Optional<E> err() {
      return Optional.empty();
    }
  }

  record Err<T, E>(@NotNull E error) implements Result<T, E> {

    public Err {
      if (error == null)
        throw new IllegalArgumentException("Err value must not be null");
    }

    @Override
    public boolean isOk() {
      return false;
    }

    @Override
    public @NotNull T unwrap() {
      throw new NoSuchElementException("Called unwrap() on Err: " + error);
    }

    @Override
    public @NotNull T unwrapOr(final @NotNull T defaultValue) {
      return defaultValue;
    }

    @Override
    public @NotNull T unwrapOrElse(final @NotNull Function<E, T> fn) {
      return fn.apply(error);
    }

    @Override
    public @NotNull E unwrapErr() {
      return error;
    }

    @Override
    public <U> @NotNull Result<U, E> map(final @NotNull Function<T, U> fn) {
      return Result.err(error);
    }

    @Override
    public <F> @NotNull Result<T, F> mapErr(final @NotNull Function<E, F> fn) {
      return Result.err(fn.apply(error));
    }

    @Override
    public <U> @NotNull Result<U, E> flatMap(final @NotNull Function<T, Result<U, E>> fn) {
      return Result.err(error);
    }

    @Override
    public @NotNull Result<T, E> ifOk(final @NotNull Consumer<T> action) {
      return this;
    }

    @Override
    public @NotNull Result<T, E> ifErr(final @NotNull Consumer<E> action) {
      action.accept(error);
      return this;
    }

    @Override
    public @NotNull Optional<T> ok() {
      return Optional.empty();
    }

    @Override
    public @NotNull Optional<E> err() {
      return Optional.of(error);
    }
  }

  @FunctionalInterface
  interface ThrowingSupplier<T, X extends Throwable> {
    T get() throws X;
  }

  static <T, X extends Throwable> @NotNull Result<T, X> of(
      final @NotNull ThrowingSupplier<T, X> supplier) {
    try {
      return ok(supplier.get());
    } catch (Throwable e) {
      @SuppressWarnings("unchecked")
      X error = (X) e;
      return err(error);
    }
  }
}
