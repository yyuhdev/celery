package club.revived.celery.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import club.revived.celery.database.model.DatabaseCredentials;
import club.revived.celery.database.model.Entity;
import club.revived.celery.database.model.LogMetric;
import club.revived.celery.database.provider.DatabaseRegistry;
import club.revived.celery.database.provider.impl.InfluxDBProvider;
import club.revived.celery.database.provider.impl.MongoDatabaseProvider;

public final class CompositioBuilder {

  @Nullable
  private DatabaseCredentials mongoCredentials;

  @Nullable
  private DatabaseCredentials influxCredentials;

  CompositioBuilder() {
  }

  @NotNull
  public CompositioBuilder mongo(final @NotNull DatabaseCredentials credentials) {
    this.mongoCredentials = credentials;
    return this;
  }

  @NotNull
  public CompositioBuilder mongo() {
    return mongo(DatabaseCredentialsEnv.mongo());
  }

  @NotNull
  public CompositioBuilder influx(final @NotNull DatabaseCredentials credentials) {
    this.influxCredentials = credentials;
    return this;
  }

  @NotNull
  public CompositioBuilder influx() {
    return influx(DatabaseCredentialsEnv.influx());
  }

  @NotNull
  public Compositio build() {
    final var registry = new DatabaseRegistry();

    if (mongoCredentials != null) {
      registry.register(Entity.class, new MongoDatabaseProvider(), mongoCredentials);
    }

    if (influxCredentials != null) {
      registry.register(LogMetric.class, new InfluxDBProvider(), influxCredentials);
    }

    return new Compositio(registry);
  }
}
