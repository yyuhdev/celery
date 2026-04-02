package club.revived.celery.database.provider.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.Sorts;

import club.revived.celery.database.codec.EntityCodecProvider;
import club.revived.celery.database.model.DatabaseCredentials;
import club.revived.celery.database.model.Entity;
import club.revived.celery.database.model.annotation.Repository;
import club.revived.celery.database.model.query.QueryFilter;
import club.revived.celery.database.provider.DatabaseProvider;

public final class MongoDatabaseProvider implements DatabaseProvider<Entity> {

  @Nullable
  private MongoClient mongoClient;

  @Nullable
  private MongoDatabase database;

  @Nullable
  private CodecRegistry codecRegistry;

  private boolean connected = false;

  @Override
  public void connect(final @NotNull DatabaseCredentials credentials) {
    try {
      final String connectionString;

      if (credentials.password() != null && !credentials.password().isEmpty()) {
        connectionString = String.format(
            "mongodb+srv://%s:%s@%s/%s",
            credentials.user(),
            credentials.password(),
            credentials.host(),
            credentials.database());
      } else {
        connectionString = String.format(
            "mongodb+srv://%s/%s",
            credentials.host(),
            credentials.database());
      }

      codecRegistry = CodecRegistries.fromRegistries(
          MongoClientSettings.getDefaultCodecRegistry(),
          CodecRegistries.fromProviders(new EntityCodecProvider()));

      final var settings = MongoClientSettings.builder()
          .applyConnectionString(new ConnectionString(connectionString))
          .codecRegistry(codecRegistry)
          .uuidRepresentation(UuidRepresentation.STANDARD)
          .build();

      mongoClient = MongoClients.create(settings);
      database = mongoClient.getDatabase(credentials.database());
      connected = true;
    } catch (final Exception e) {
      connected = false;
      throw new RuntimeException("Failed to connect to MongoDB", e);
    }
  }

  @Override
  @NotNull
  public CompletableFuture<Void> write(final @NotNull Entity entity) {
    return CompletableFuture.runAsync(() -> {
      requireConnection();

      final var name = getCollection(entity.getClass());
      final MongoCollection<Entity> collection = database.getCollection(name)
          .withDocumentClass(Entity.class)
          .withCodecRegistry(codecRegistry);

      collection.insertOne(entity, new InsertOneOptions());
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> writeBatch(final @NotNull List<Entity> entities) {
    return CompletableFuture.runAsync(() -> {
      if (entities.isEmpty()) {
        return;
      }

      requireConnection();

      final var name = getCollection(Entity.class);
      final MongoCollection<Entity> collection = database.getCollection(name)
          .withDocumentClass(Entity.class)
          .withCodecRegistry(codecRegistry);

      collection.insertMany(entities, new InsertManyOptions());
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Optional<Entity>> find(final @NotNull QueryFilter<Entity> filter) {
    return CompletableFuture.supplyAsync(() -> {
      requireConnection();

      final var name = getCollection(Entity.class);
      final MongoCollection<Entity> collection = database.getCollection(name)
          .withDocumentClass(Entity.class)
          .withCodecRegistry(codecRegistry);

      final var bsonFilter = buildBsonFilter(filter);
      final var entity = collection.find(bsonFilter).first();

      return Optional.ofNullable(entity);
    });
  }

  @Override
  @NotNull
  public CompletableFuture<List<Entity>> findBatch(
      final @NotNull Collection<? extends QueryFilter<Entity>> filters) {
    return CompletableFuture.supplyAsync(() -> {
      requireConnection();

      final var name = getCollection(Entity.class);
      final MongoCollection<Entity> collection = database.getCollection(name)
          .withDocumentClass(Entity.class)
          .withCodecRegistry(codecRegistry);

      return filters.stream()
          .map(f -> {
            final var bsonFilter = buildBsonFilter(f);
            return collection.find(bsonFilter).into(new ArrayList<>());
          })
          .flatMap(List::stream)
          .toList();
    });
  }

  @Override
  @NotNull
  public CompletableFuture<List<Entity>> findAll() {
    return CompletableFuture.supplyAsync(() -> {
      requireConnection();

      final var name = getCollection(Entity.class);
      final MongoCollection<Entity> collection = database.getCollection(name)
          .withDocumentClass(Entity.class)
          .withCodecRegistry(codecRegistry);

      return collection.find().into(new ArrayList<>());
    });
  }

  @Override
  @NotNull
  public CompletableFuture<List<Entity>> findAll(final @NotNull QueryFilter<Entity> filter) {
    return CompletableFuture.supplyAsync(() -> {
      requireConnection();

      final var name = getCollection(Entity.class);
      final MongoCollection<Entity> collection = database.getCollection(name)
          .withDocumentClass(Entity.class)
          .withCodecRegistry(codecRegistry);

      final var bsonFilter = buildBsonFilter(filter);
      final var query = collection.find(bsonFilter);

      if (filter.sort() != null) {
        final var sort = filter.sort().direction() == QueryFilter.Direction.ASCENDING
            ? Sorts.ascending(filter.sort().field())
            : Sorts.descending(filter.sort().field());
        query.sort(sort);
      }

      if (filter.limit() != null) {
        query.limit(filter.limit());
      }

      return query.into(new ArrayList<>());
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> delete(final @NotNull QueryFilter<Entity> filter) {
    return CompletableFuture.runAsync(() -> {
      requireConnection();

      final var name = getCollection(Entity.class);
      final MongoCollection<Entity> collection = database.getCollection(name)
          .withDocumentClass(Entity.class)
          .withCodecRegistry(codecRegistry);

      final var bsonFilter = buildBsonFilter(filter);
      collection.deleteMany(bsonFilter);
    });
  }

  private void requireConnection() {
    if (!connected || mongoClient == null || database == null) {
      throw new IllegalStateException("MongoDB is not connected.");
    }
  }

  @NotNull
  private <T extends Entity> String getCollection(final @NotNull Class<T> clazz) {
    final var simpleName = clazz.getSimpleName().toLowerCase();

    try {
      if (clazz.isAnnotationPresent(Repository.class)) {
        final var name = clazz.getAnnotation(Repository.class).value();
        return name.isEmpty() ? simpleName : name;
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to retrieve collection name from entity", e);
    }

    return simpleName;
  }

  @NotNull
  private Bson buildBsonFilter(final @NotNull QueryFilter<Entity> filter) {
    final List<Bson> bsonConditions = filter.conditions().stream()
        .map(cond -> switch (cond.operator()) {
          case EQ -> Filters.eq(cond.field(), cond.value());
          case NE -> Filters.ne(cond.field(), cond.value());
          case GT -> Filters.gt(cond.field(), cond.value());
          case GTE -> Filters.gte(cond.field(), cond.value());
          case LT -> Filters.lt(cond.field(), cond.value());
          case LTE -> Filters.lte(cond.field(), cond.value());
          case IN -> Filters.in(cond.field(), (Collection<?>) cond.value());
          case CONTAINS -> Filters.regex(cond.field(), ".*" + cond.value() + ".*");
          case STARTS_WITH -> Filters.regex(cond.field(), "^" + cond.value());
          case ENDS_WITH -> Filters.regex(cond.field(), cond.value() + "$");
          case EXISTS -> Filters.exists(cond.field());
          case NOT_EXISTS -> Filters.exists(cond.field(), false);
        }).toList();

    return bsonConditions.isEmpty() ? Filters.empty() : Filters.and(bsonConditions);
  }
}
