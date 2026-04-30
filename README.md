<div align="center">
  <img src="img/logo.png" alt="Longinus Logo" height="240">
</div>

# Celery

A modular database abstraction framework for Java. Celery provides a unified API across multiple database backends while remaining fully extensible.

## Architecture

### Module Structure

```
celery/
├── core/                    # Core utilities (Result<T,E>, Timer)
├── celery-api/              # Interfaces and abstractions
├── celery/                  # Main entry point (Celery builder)
└── celery-platform/         # Platform implementations
    ├── celery-mongodb/      # MongoDB implementation
    └── celery-redis/        # Redis implementation
```

### Core Concepts

**Platforms**: A platform represents a specific database type (MongoDB, Redis, etc.) and exposes providers for different operations (database, messaging, cache).

**Providers**: Implementations of specific capabilities (e.g., `IDatabaseProvider` for CRUD, `IMessagingProvider` for pub/sub).

**Platform Types**: Categories that define how a platform is used:
- `STORAGE` - Primary data storage
- `PUBSUB` - Message publishing/subscribing
- `CACHE` - Caching layer

## Adding a Custom Platform

### 1. Create a Database Type

Implement `IDatabaseType` to define your database:

```java
public enum MyDatabaseType implements IDatabaseType {
    MYSQL {
        @Override
        public int defaultPort() {
            return 3306;
        }

        @Override
        public @NotNull CeleryPlatformType defaultPlatform() {
            return CeleryPlatformType.STORAGE;
        }
    };

    public abstract int defaultPort();
}
```

Or simply add to `CeleryDatabaseType` enum if you want it built-in.

### 2. Implement IProvider

Base interface for all providers:

```java
public class MyDatabaseProvider implements IDatabaseProvider<IEntity, IQuery<IEntity>> {
    @Override
    public @NotNull CompletableFuture<Result<Long, String>> connect(Credentials credentials) {
        // Initialize connection
    }

    @Override
    public @NotNull CompletableFuture<Boolean> isConnected() {
        // Check connection status
    }

    @Override
    public void close() {
        // Cleanup resources
    }
}

```

### 3. Automatic reconnecting

Alongside IDatabaseProvider there is the IReconnectable interface allowing you to automaticly connect back to your database when connection is lost. It works right out of the box

```java

public class MyDatabaseProvider implements IDatabaseProvider<IEntity, IQuery<Entity>>, IReconnectable {
    @Override
    public @NotNull CompletableFuture<Result<Long, String>> connect(Credentials credentials) {
        // Initialize connection
    }

    @Override
    public @NotNull CompletableFuture<Boolean> isConnected() {
        // Check connection status
    }

    @Override
    public void close() {
        // Cleanup resources
    }
}

```

### 4. Extend AbstractCeleryPlatform

```java
public class MyPlatform extends AbstractCeleryPlatform {
    public MyPlatform() {
        super("mysql", MyDatabaseType.MYSQL, CeleryPlatformType.STORAGE);
        registerProvider(IDatabaseProvider.class, new MyDatabaseProvider());
    }

    @Override
    public @NotNull IProvider defaultProvider() {
        return provider(IDatabaseProvider.class).orElseThrow();
    }
}
```

### 5. Register with Celery

```java
Celery.create()
    .registerCredentialProvider(new EnvCredentialProvider())
    .registerPlatform(MyPlatform.class)
    .build();
```

## Adding Custom Providers

Beyond database providers, you can register any provider type:

```java
public class MyCustomProvider implements IProvider {
    // implementation
}

platform.registerProvider(MyCustomProvider.class, new MyCustomProvider());
```

Retrieve it later:
```java
MyCustomProvider provider = platform.provider(MyCustomProvider.class).orElseThrow();
```

## Credential Providers

Implement `ICredentialProvider` to handle authentication:

```java
public class MyCredentialProvider implements ICredentialProvider {
    @Override
    public @NotNull Optional<Credentials> create(IDatabaseType databaseType) {
        if (databaseType == MyDatabaseType.MYSQL) {
            return Optional.of(new Credentials(
                System.getenv("MYSQL_HOST"),
                System.getenv("MYSQL_USER"),
                System.getenv("MYSQL_PASS"),
                3306
            ));
        }
        return Optional.empty();
    }
}
```

## Entity System

Use annotations to map your domain models:

```java
@Repository("users")
public class User implements IEntity {
    @Identifier
    private String id;

    @Field
    private String name;

    @Field
    private String email;

    @Ignore
    private transient String password; // Not persisted
}
```

## Event Bus

High-performance event distribution:

```java
EventBus eventBus = celery.eventBus();
eventBus.register(MyEvent.class, event -> { /* handle */ });
eventBus.post(new MyEvent());
```

## Build

```bash
./gradlew build
```

<img src="https://yyuh.beer/banners/vim-tenor.gif" width="88" height="33" />
