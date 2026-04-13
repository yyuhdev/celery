package de.yyuh.celery.platform.mongodb.provider;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.yyuh.celery.api.annotation.Identifier;
import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.provider.IDatabaseProvider;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.celery.api.schema.SchemaExtractor;
import de.yyuh.celery.platform.mongodb.codec.EntityCodecProvider;
import de.yyuh.celery.platform.mongodb.query.BsonQueryBuilder;
import de.yyuh.libs.core.result.Result;
import de.yyuh.libs.core.timer.Timer;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * MongoDB implementation of IDatabaseProvider for IEntity types.
 *
 * <p>
 * This provider handles CRUD operations for entities stored in MongoDB,
 * using the EntityCodec for encoding and decoding record types.
 */
public final class MongoDatabaseProvider implements IDatabaseProvider<IEntity, IQuery<IEntity>> {

  private MongoClient mongoClient;
  private MongoDatabase database;

  @Override
  public @NotNull CompletableFuture<Result<Long, String>> connect(final @NotNull Credentials credentials) {
    final var timer = Timer.start();

    return CompletableFuture.supplyAsync(() -> Result.of(() -> {
      final String connectionString = String.format("mongodb://%s:%s@%s:%d",
          credentials.user(), credentials.password(), credentials.ip(), credentials.port());

      final CodecRegistry pojoCodecRegistry = CodecRegistries.fromProviders(new EntityCodecProvider());
      final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
          MongoClientSettings.getDefaultCodecRegistry(),
          pojoCodecRegistry);

      final MongoClientSettings settings = MongoClientSettings.builder()
          .applyConnectionString(new ConnectionString(connectionString))
          .codecRegistry(codecRegistry)
          .build();

      this.mongoClient = MongoClients.create(settings);
      this.database = mongoClient.getDatabase("celery");

      return timer.end();
    }).mapErr(Exception::getMessage));
  }

  @Override
  public @NotNull CompletableFuture<Optional<IEntity>> get(final @NotNull IQuery<IEntity> iQuery) {
    return CompletableFuture.supplyAsync(() -> {
      final String collectionName = SchemaExtractor.getName(iQuery.entityClass());
      final IEntity entity = (IEntity) database.getCollection(collectionName, iQuery.entityClass())
          .find(BsonQueryBuilder.buildFilter(iQuery))
          .first();
      return Optional.ofNullable(entity);
    });
  }

  @Override
  public @NotNull CompletableFuture<List<IEntity>> find(final @NotNull IQuery<IEntity> query) {
    return CompletableFuture.supplyAsync(() -> {
      final String collectionName = SchemaExtractor.getName(query.entityClass());
      final List<IEntity> results = new ArrayList<>();
      database.getCollection(collectionName, query.entityClass())
          .find(BsonQueryBuilder.buildFilter(query))
          .into(results);
      return results;
    });
  }

  @Override
  public @NotNull CompletableFuture<List<IEntity>> getAll() {
    return CompletableFuture.completedFuture(new ArrayList<>());
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull CompletableFuture<Void> save(final @NotNull IEntity entity) {
    return CompletableFuture.runAsync(() -> {
      final String collectionName = SchemaExtractor.getName(entity.getClass());
      final Object id = this.extractId(entity);

      database.getCollection(collectionName, (Class<IEntity>) entity.getClass())
          .replaceOne(new org.bson.Document("_id", id), entity,
              new com.mongodb.client.model.ReplaceOptions().upsert(true));
    });
  }

  /**
   * Extracts the identifier value from an entity.
   *
   * <p>
   * For record types, the identifier is retrieved from record components
   * annotated with @Identifier. For regular classes, it is retrieved from
   * fields annotated with @Identifier.
   *
   * @param entity the entity to extract the identifier from
   * @return the identifier value
   * @throws IllegalArgumentException if the entity has no @Identifier annotation
   * @throws RuntimeException         if the identifier cannot be accessed
   */
  @NotNull
  private Object extractId(final @NotNull IEntity entity) {
    if (entity.getClass().isRecord()) {
      for (final var component : entity.getClass().getRecordComponents()) {
        if (component.isAnnotationPresent(Identifier.class)) {
          try {
            final Object value = component.getAccessor().invoke(entity);
            if (value == null) {
              throw new IllegalArgumentException("Identifier value for " + entity.getClass().getName() + " is null");
            }
            return value;
          } catch (Exception exception) {
            throw new RuntimeException("Failed to access identifier for " + entity.getClass().getName(), exception);
          }
        }
      }
    } else {
      for (final var field : entity.getClass().getDeclaredFields()) {
        if (field.isAnnotationPresent(Identifier.class)) {
          field.setAccessible(true);
          try {
            final Object value = field.get(entity);
            if (value == null) {
              throw new IllegalArgumentException("Identifier value for " + entity.getClass().getName() + " is null");
            }
            return value;
          } catch (IllegalAccessException exception) {
            throw new RuntimeException("Failed to access identifier for " + entity.getClass().getName(), exception);
          }
        }
      }
    }

    throw new IllegalArgumentException(
        "Could not find @Identifier annotation for entity " + entity.getClass().getName());
  }

  @Override
  public @NotNull CompletableFuture<Void> delete(final @NotNull IQuery<IEntity> iQuery) {
    return CompletableFuture.runAsync(() -> {
      final String collectionName = SchemaExtractor.getName(iQuery.entityClass());

      database.getCollection(collectionName).deleteOne(BsonQueryBuilder.buildFilter(iQuery));
    });
  }

  @Override
  public void close() {
    this.mongoClient.close();
  }

  @Override
  public CompletableFuture<Boolean> isConnected() {
    return CompletableFuture.supplyAsync(() -> {
      if (this.mongoClient == null) {
        return false;
      }

      return Result.of(() -> this.mongoClient.listDatabaseNames().first())
          .map(ignored -> true)
          .unwrapOr(false);
    });
  }
}
