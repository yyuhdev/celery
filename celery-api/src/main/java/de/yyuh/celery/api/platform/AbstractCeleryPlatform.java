package de.yyuh.celery.api.platform;

import de.yyuh.celery.api.CeleryPlatformType;
import de.yyuh.celery.api.IDatabaseType;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.provider.IDatabaseProvider;
import de.yyuh.celery.api.query.IQuery;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractCeleryPlatform {

    private final String id;
    private final IDatabaseType databaseType;
    private final CeleryPlatformType celeryPlatformType;

    public AbstractCeleryPlatform(
            final @NotNull String id,
            final @NotNull IDatabaseType databaseType,
            final @NotNull CeleryPlatformType platformType) {
        this.id = id;
        this.databaseType = databaseType;
        this.celeryPlatformType = platformType;
    }

    @NotNull
    public String getId() {
        return this.id;
    }

    @NotNull
    public IDatabaseType getDatabaseType() {
        return this.databaseType;
    }

    @NotNull
    public CeleryPlatformType getCeleryPlatformType() {
        return this.celeryPlatformType;
    }

    public abstract IDatabaseProvider<IEntity, IQuery> defaultProvider();
}
