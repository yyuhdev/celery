package club.revived.commons.result;

import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface Result<T, E extends Throwable> permits Ok, Err {

  @FunctionalInterface
  interface ThrowingSupplier<T, E extends Throwable> {
    @NotNull
    T get() throws E;
  }

  @NotNull
  static <T, E extends Throwable> Result<@NotNull T, E> of(final @NotNull ThrowingSupplier<@NotNull T, E> supplier) {
    try {
      return ok(supplier.get());
    } catch (final Throwable e) {
      @SuppressWarnings("unchecked")
      final E error = (E) e;

      return err(error);
    }
  }

  @NotNull
  static <T, E extends Throwable> Result<@NotNull T, E> ok(final @NotNull T value) {
    return new Ok<>(value);
  }

  @NotNull
  static <T, E extends Throwable> Result<T, @NotNull E> err(final @NotNull E error) {
    return new Err<>(error);
  }

  boolean isOk();

  boolean isErr();

  @NotNull
  T unwrap() throws E;

  @NotNull
  E unwrapErr();

  @NotNull
  T unwrapOr(final @NotNull T defaultValue);

  @NotNull
  T unwrapOrElse(final @NotNull Function<@NotNull E, @NotNull T> mapper);

  @NotNull
  <U> Result<@NotNull U, E> map(final @NotNull Function<@NotNull T, @NotNull U> mapper);

  @NotNull
  <F extends Throwable> Result<T, @NotNull F> mapErr(final @NotNull Function<@NotNull E, @NotNull F> mapper);

  @NotNull
  <U> Result<@NotNull U, E> flatMap(final @NotNull Function<@NotNull T, @NotNull Result<@NotNull U, E>> mapper);

  @NotNull
  Result<T, E> ifOk(final @NotNull Consumer<@NotNull T> action);

  @NotNull
  Result<T, E> ifErr(final @NotNull Consumer<@NotNull E> action);

  @NotNull
  Optional<@NotNull T> ok();

  @NotNull
  Optional<@NotNull E> err();
}

record Ok<T, E extends Throwable>(@NotNull T value) implements Result<T, E> {

  @Override
  public boolean isOk() {
    return true;
  }

  @Override
  public boolean isErr() {
    return false;
  }

  @Override
  public @NotNull T unwrap() {
    return value;
  }

  @Override
  public @NotNull E unwrapErr() {
    throw new NoSuchElementException("Called unwrapErr on an Ok value");
  }

  @Override
  public @NotNull T unwrapOr(final @NotNull T defaultValue) {
    return value;
  }

  @Override
  public @NotNull T unwrapOrElse(final @NotNull Function<@NotNull E, @NotNull T> mapper) {
    return value;
  }

  @Override
  public <U> @NotNull Result<@NotNull U, E> map(final @NotNull Function<@NotNull T, @NotNull U> mapper) {
    return Result.ok(mapper.apply(value));
  }

  @Override
  public <F extends Throwable> @NotNull Result<T, @NotNull F> mapErr(
      final @NotNull Function<@NotNull E, @NotNull F> mapper) {
    return Result.ok(value);
  }

  @Override
  public <U> @NotNull Result<@NotNull U, E> flatMap(
      final @NotNull Function<@NotNull T, @NotNull Result<@NotNull U, E>> mapper) {
    return mapper.apply(value);
  }

  @Override
  public @NotNull Result<T, E> ifOk(final @NotNull Consumer<@NotNull T> action) {
    action.accept(value);
    return this;
  }

  @Override
  public @NotNull Result<T, E> ifErr(final @NotNull Consumer<@NotNull E> action) {
    return this;
  }

  @Override
  public @NotNull Optional<@NotNull T> ok() {
    return Optional.of(value);
  }

  @Override
  public @NotNull Optional<@NotNull E> err() {
    return Optional.empty();
  }
}

record Err<T, E extends Throwable>(@NotNull E error) implements Result<T, E> {

  @Override
  public boolean isOk() {
    return false;
  }

  @Override
  public boolean isErr() {
    return true;
  }

  @Override
  public @NotNull T unwrap() throws E {
    throw error;
  }

  @Override
  public @NotNull E unwrapErr() {
    return error;
  }

  @Override
  public @NotNull T unwrapOr(final @NotNull T defaultValue) {
    return defaultValue;
  }

  @Override
  public @NotNull T unwrapOrElse(final @NotNull Function<@NotNull E, @NotNull T> mapper) {
    return mapper.apply(error);
  }

  @Override
  public <U> @NotNull Result<@NotNull U, E> map(final @NotNull Function<@NotNull T, @NotNull U> mapper) {
    return Result.err(error);
  }

  @Override
  public <F extends Throwable> @NotNull Result<T, @NotNull F> mapErr(
      final @NotNull Function<@NotNull E, @NotNull F> mapper) {
    return Result.err(mapper.apply(error));
  }

  @Override
  public <U> @NotNull Result<@NotNull U, E> flatMap(
      final @NotNull Function<@NotNull T, @NotNull Result<@NotNull U, E>> mapper) {
    return Result.err(error);
  }

  @Override
  public @NotNull Result<T, E> ifOk(final @NotNull Consumer<@NotNull T> action) {
    return this;
  }

  @Override
  public @NotNull Result<T, E> ifErr(final @NotNull Consumer<@NotNull E> action) {
    action.accept(error);
    return this;
  }

  @Override
  public @NotNull Optional<@NotNull T> ok() {
    return Optional.empty();
  }

  @Override
  public @NotNull Optional<@NotNull E> err() {
    return Optional.of(error);
  }
}
