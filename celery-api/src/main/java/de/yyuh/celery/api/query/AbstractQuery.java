package de.yyuh.celery.api.query;

import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractQuery<T> implements IQuery<T> {

    private final Class<T> entityClass;
    private final Map<String, Object> filters;
    private final Integer limit;
    private final Integer offset;

    protected AbstractQuery(Builder<T, ?> builder) {
        this.entityClass = builder.entityClass;
        this.filters = Collections.unmodifiableMap(new HashMap<>(builder.filters));
        this.limit = builder.limit;
        this.offset = builder.offset;
    }

    @Override
    public @NotNull Class<T> entityClass() {
        return entityClass;
    }

    @Override
    public @NotNull Map<String, Object> filters() {
        return filters;
    }

    @Override
    public @NotNull Optional<Integer> limit() {
        return Optional.ofNullable(limit);
    }

    @Override
    public @NotNull Optional<Integer> offset() {
        return Optional.ofNullable(offset);
    }

    @SuppressWarnings("unchecked")
    public abstract static class Builder<T, B extends Builder<T, B>> implements IQuery.Builder<T, B> {
        protected final Class<T> entityClass;
        protected final Map<String, Object> filters = new HashMap<>();
        protected Integer limit;
        protected Integer offset;

        protected Builder(@NotNull Class<T> entityClass) {
            this.entityClass = entityClass;
        }

        @Override
        public @NotNull B filter(@NotNull String key, @NotNull Object value) {
            this.filters.put(key, value);
            return (B) this;
        }

        @Override
        public @NotNull B limit(int limit) {
            this.limit = limit;
            return (B) this;
        }

        @Override
        public @NotNull B offset(int offset) {
            this.offset = offset;
            return (B) this;
        }
    }
}
