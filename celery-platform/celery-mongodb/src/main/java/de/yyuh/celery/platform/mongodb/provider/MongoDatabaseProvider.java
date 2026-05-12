package de.yyuh.celery.platform.mongodb.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.jetbrains.annotations.NotNull;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.provider.IDatabaseProvider;
import de.yyuh.celery.api.provider.IReconnectable;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.celery.api.schema.SchemaExtractor;
import de.yyuh.celery.platform.mongodb.codec.EntityCodecProvider;
import de.yyuh.celery.platform.mongodb.query.BsonQueryBuilder;
import de.yyuh.libs.core.result.Result;
import de.yyuh.libs.core.timer.Timer;

/**
 * MongoDB implementation of IDatabaseProvider for IEntity types.
 *
 * <p>
 * This provider handles CRUD operations for entities stored in MongoDB,
 * using the EntityCodec for encoding and decoding record types.
 */
public final class MongoDatabaseProvider implements IReconnectable, IDatabaseProvider<IEntity, IQuery<IEntity>> {

  private MongoClient mongoClient;
  private MongoDatabase database;

  /** {@inheritDoc} */
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
          .uuidRepresentation(UuidRepresentation.STANDARD)
          .codecRegistry(codecRegistry)
          .build();

      this.mongoClient = MongoClients.create(settings);
      this.database = mongoClient.getDatabase(credentials.database());

      return timer.end();
    }).mapErr(Exception::getMessage));
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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
  @Deprecated(forRemoval = true)
  public @NotNull CompletableFuture<List<IEntity>> getAll() {
    throw new UnsupportedOperationException("getAll is unsupported for the IEntity type!");
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull CompletableFuture<Void> save(final @NotNull IEntity entity) {
    return CompletableFuture.runAsync(() -> {
      final String collectionName = SchemaExtractor.getName(entity.getClass());
      final Object id = SchemaExtractor.extractId(entity);

      database.getCollection(collectionName, (Class<IEntity>) entity.getClass())
          .replaceOne(new Document("_id", id), entity,
              new ReplaceOptions().upsert(true));
    });
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Void> delete(final @NotNull IQuery<IEntity> iQuery) {
    return CompletableFuture.runAsync(() -> {
      final String collectionName = SchemaExtractor.getName(iQuery.entityClass());

      database.getCollection(collectionName).deleteOne(BsonQueryBuilder.buildFilter(iQuery));
    });
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    if (this.mongoClient != null) {
      this.mongoClient.close();
    }
  }

  /** {@inheritDoc} */
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
