package de.yyuh.celery.api.query;

import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.Optional;

public interface IQuery<T> {

    @NotNull
    Class<T> entityClass();

    @NotNull
    Map<String, Object> filters();

    @NotNull
    Optional<Integer> limit();

    @NotNull
    Optional<Integer> offset();

    interface Builder<T, B extends Builder<T, B>> {
        @NotNull
        B filter(@NotNull String key, @NotNull Object value);

        @NotNull
        B limit(int limit);

        @NotNull
        B offset(int offset);

        @NotNull
        IQuery<T> build();
    }
}
