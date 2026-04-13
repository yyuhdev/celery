package de.yyuh.libs.core.result;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a result that can be either successful (Ok) or failed (Err).
 *
 * <p>This sealed interface provides functional error handling similar to
 * Rust's Result type. The Ok variant contains a success value, while
 * the Err variant contains an error value.
 *
 * @param <T> the type of the success value
 * @param <E> the type of the error value
 */
public sealed interface Result<T, E> permits Result.Ok, Result.Err {

  /**
   * Creates a successful result containing a value.
   *
   * @param value the success value
   * @param <T> the value type
   * @param <E> the error type
   * @return an Ok result
   */
  static <T, E> @NotNull Result<T, E> ok(final @NotNull T value) {
    return new Ok<>(value);
  }

  /**
   * Creates an error result containing an error.
   *
   * @param error the error value
   * @param <T> the value type
   * @param <E> the error type
   * @return an Err result
   */
  static <T, E> @NotNull Result<T, E> err(final @NotNull E error) {
    return new Err<>(error);
  }

  boolean isOk();

  default boolean isErr() {
    return !isOk();
  }

  /**
   * Unwraps the value, throwing if this is an Err.
   *
   * @return the success value
   * @throws NoSuchElementException if this is an Err
   */
  @NotNull
  T unwrap();

  /**
   * Unwraps the value or returns a default if this is an Err.
   *
   * @param defaultValue the value to return if this is an Err
   * @return the value or the default
   */
  @NotNull
  T unwrapOr(final @NotNull T defaultValue);

  /**
   * Unwraps the value or computes it from the error if this is an Err.
   *
   * @param fn the function to apply to the error value
   * @return the value or the computed result
   */
  @NotNull
  T unwrapOrElse(final @NotNull Function<E, T> fn);

  /**
   * Unwraps the error value, throwing if this is an Ok.
   *
   * @return the error value
   * @throws NoSuchElementException if this is an Ok
   */
  @NotNull
  E unwrapErr();

  /**
   * Maps the success value using the provided function.
   *
   * @param fn the function to apply to the value
   * @param <U> the new value type
   * @return a new Result with the mapped value
   */
  <U> @NotNull Result<U, E> map(final @NotNull Function<T, U> fn);

  /**
   * Maps the error value using the provided function.
   *
   * @param fn the function to apply to the error
   * @param <F> the new error type
   * @return a new Result with the mapped error
   */
  <F> @NotNull Result<T, F> mapErr(final @NotNull Function<E, F> fn);

  /**
   * Chains Result computations, flattening nested results.
   *
   * @param fn the function to apply to the value
   * @param <U> the new value type
   * @return the result of the chained computation
   */
  <U> @NotNull Result<U, E> flatMap(final @NotNull Function<T, Result<U, E>> fn);

  /**
   * Executes an action if this is an Ok, returning this for chaining.
   *
   * @param action the action to execute
   * @return this Result
   */
  @NotNull
  Result<T, E> ifOk(final @NotNull Consumer<T> action);

  /**
   * Executes an action if this is an Err, returning this for chaining.
   *
   * @param action the action to execute
   * @return this Result
   */
  @NotNull
  Result<T, E> ifErr(final @NotNull Consumer<E> action);

  /**
   * Returns an Optional containing the value if Ok, or empty if Err.
   *
   * @return an Optional with the value
   */
  @NotNull
  Optional<T> ok();

  /**
   * Returns an Optional containing the error if Err, or empty if Ok.
   *
   * @return an Optional with the error
   */
  @NotNull
  Optional<E> err();

  /**
   * Represents a successful result containing a value.
   *
   * @param <T> the value type
   * @param <E> the error type
   */
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

  /**
   * Represents an error result containing an error value.
   *
   * @param <T> the value type
   * @param <E> the error type
   */
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

  /**
   * A supplier that may throw an exception.
   *
   * @param <T> the result type
   * @param <X> the exception type
   */
  @FunctionalInterface
  interface ThrowingSupplier<T, X extends Throwable> {
    T get() throws X;
  }

  /**
   * Converts a throwing supplier to a Result.
   *
   * @param supplier the throwing supplier
   * @param <T> the result type
   * @param <X> the exception type
   * @return an Ok with the result or an Err with the exception
   */
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
