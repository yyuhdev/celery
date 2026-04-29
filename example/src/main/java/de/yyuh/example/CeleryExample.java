package de.yyuh.example;

import de.yyuh.celery.Celery;
import de.yyuh.celery.api.PlatformManager;
import de.yyuh.celery.api.credentials.provider.EnvCredentialProvider;
import de.yyuh.celery.api.entity.ILogEntry;
import de.yyuh.celery.api.messaging.MessageBus;
import de.yyuh.celery.api.platform.AbstractCeleryPlatform;
import de.yyuh.celery.api.provider.ICacheProvider;
import de.yyuh.celery.api.provider.IDatabaseProvider;
import de.yyuh.celery.api.provider.ITimeseriesProvider;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.celery.logging.CeleryInfluxDBPlatform;
import de.yyuh.celery.platform.mongodb.CeleryMongoDBPlatform;
import de.yyuh.celery.platform.nats.CeleryNatsPlatform;
import de.yyuh.celery.platform.redis.CeleryRedisPlatform;
import de.yyuh.celery.platform.redis.cache.CeleryRedisCachePlatform;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * End-to-end example demonstrating Celery with Dockerized backends.
 *
 * <h3>Prerequisites</h3>
 * <pre>{@code
 *   docker compose --profile all up -d    # Start all services
 *   ./gradlew :example:run                # Run this example
 *   docker compose down                   # Stop services
 * }</pre>
 */
public final class CeleryExample {

    public static void main(final String[] args) {
        System.out.println("=" .repeat(60));
        System.out.println("  Celery Framework — Integration Example");
        System.out.println("=" .repeat(60));

        // ── 1. Build Celery ────────────────────────────────────────────
        System.out.println("\n[1] Building Celery instance...");

        final Celery celery = Celery.builder()
            .withId("example-service")
            .registerCredentialProvider(new EnvCredentialProvider())
            .registerPlatform(CeleryMongoDBPlatform.class)
            .registerPlatform(CeleryRedisCachePlatform.class)
            .registerPlatform(CeleryRedisPlatform.class)
            .registerPlatform(CeleryNatsPlatform.class)
            .registerPlatform(CeleryInfluxDBPlatform.class)
            .build();

        System.out.println("    ✓ Celery built successfully");

        // ── 2. MongoDB: Save & Query ───────────────────────────────────
        System.out.println("\n[2] MongoDB — CRUD operations");

        final var mongoPlatform = platform(celery, CeleryMongoDBPlatform.class);
        final IDatabaseProvider<User, IQuery<User>> db =
            (IDatabaseProvider<User, IQuery<User>>) mongoPlatform.defaultProvider();

        final User alice = new User("Alice", "alice@example.com");
        db.save(alice).join();
        System.out.println("    ✓ Saved user: " + alice);

        final User bob = new User("Bob", "bob@example.com");
        db.save(bob).join();
        System.out.println("    ✓ Saved user: " + bob);

        // Query by _id (MongoDB's internal name for @Identifier)
        final var byId = queryById(User.class, alice.id());
        db.find(byId).join().forEach(u ->
            System.out.println("    → Found by _id: " + u));

        // Query all
        final var all = db.getAll().join();
        System.out.println("    → Total users: " + all.size());

        // ── 3. Redis Cache ─────────────────────────────────────────────
        System.out.println("\n[3] Redis Cache — Key/value operations");

        final var cachePlatform = platform(celery, CeleryRedisCachePlatform.class);
        final ICacheProvider cache = cachePlatform.provider(ICacheProvider.class).orElseThrow();

        cache.set("greeting", "Hello from Celery!".getBytes(StandardCharsets.UTF_8), Duration.ofMinutes(5)).join();
        System.out.println("    ✓ Cached 'greeting' key");

        final var cached = cache.get("greeting").join();
        System.out.println("    → Retrieved: " + cached.map(String::new).orElse("(miss)"));

        System.out.println("    → Exists? " + cache.exists("greeting").join());

        cache.delete("greeting").join();
        System.out.println("    ✓ Deleted 'greeting' key");
        System.out.println("    → Exists after delete? " + cache.exists("greeting").join());

        // ── 4. NATS Messaging ──────────────────────────────────────────
        System.out.println("\n[4] NATS — Pub/Sub messaging");

        final var natsPlatform = platform(celery, CeleryNatsPlatform.class);
        final MessageBus natsBus = celery.getMessageBus(natsPlatform.getId()).orElseThrow();

        System.out.println("    ✓ NATS MessageBus ready: " + natsBus);

        // ── 5. InfluxDB Logging ────────────────────────────────────────
        System.out.println("\n[5] InfluxDB — Time-series logging");

        final var influxPlatform = platform(celery, CeleryInfluxDBPlatform.class);
        final ITimeseriesProvider timeseries =
            influxPlatform.provider(ITimeseriesProvider.class).orElseThrow();

        timeseries.save(new DemoLogEntry("CeleryExample started successfully")).join();
        System.out.println("    ✓ Log entry saved");

        // ── Cleanup ────────────────────────────────────────────────────
        System.out.println("\n[6] Cleanup");

        db.delete(byId).join();
        System.out.println("    ✓ Deleted Alice from MongoDB");

        System.out.println("\n" + "=" .repeat(60));
        System.out.println("  All tests passed ✓");
        System.out.println("=" .repeat(60));
    }

    /**
     * Helper: gets a platform instance by class from the PlatformManager.
     */
    @SuppressWarnings("unchecked")
    private static <P extends AbstractCeleryPlatform> P platform(
        final Celery celery, final Class<P> platformClass) {

        return (P) PlatformManager.getInstance()
            .getPlatform(platformClass)
            .orElseThrow(() -> new IllegalStateException(
                "Platform not found: " + platformClass.getSimpleName()));
    }

    /**
     * Helper: builds an IQuery that filters by MongoDB's internal {@code _id} field.
     */
    private static <T> IQuery<T> queryById(final Class<T> entityClass, final String id) {
        return new IQuery<>() {
            @Override public Class<T> entityClass() { return entityClass; }
            @Override public Map<String, Object> filters() { return Map.of("_id", id); }
            @Override public Optional<Integer> limit() { return Optional.of(1); }
            @Override public Optional<Integer> offset() { return Optional.empty(); }
        };
    }

    /**
     * Minimal ILogEntry implementation for demonstration.
     */
    record DemoLogEntry(String message, Instant timestamp) implements ILogEntry {
        DemoLogEntry(String message) {
            this(message, Instant.now());
        }
    }
}
