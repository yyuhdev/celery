package de.yyuh.celery.platform.nats.cluster.provider;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.NotNull;

import com.google.protobuf.Message;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.event.EventBus;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.celery.api.messaging.MessageRegistry;
import de.yyuh.celery.api.provider.IReconnectable;
import de.yyuh.libs.core.injection.Inject;
import de.yyuh.libs.core.result.Result;
import de.yyuh.libs.core.timer.Timer;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.Connection.Status;

/**
 * NATS Cluster messaging provider with multi-server cluster support.
 *
 * <p>
 * This provider connects to a NATS cluster by accepting multiple server
 * URLs via the credentials. The {@code ip} field may contain a single
 * {@code host:port} or a comma-separated list of {@code host:port} pairs.
 *
 * <p>
 * Subscriptions use NATS queue groups for load-balanced message consumption:
 * all subscribers on the same channel form a queue group, so each message
 * is delivered to exactly one subscriber in the group.
 *
 * <p>
 * Automatic failover is handled transparently by the NATS client — if one
 * server in the cluster goes down, the client reconnects to another.
 */
public final class NatsClusterMessagingProvider implements IReconnectable, IMessagingProvider {

  private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
  private Connection connection;

  private final Map<String, Dispatcher> dispatchers = new ConcurrentHashMap<>();

  private static final String QUEUE_GROUP = "celery-cluster";

  @Inject
  private EventBus eventBus;

  @Override
  public @NotNull CompletableFuture<Void> publish(
      final @NotNull String channel,
      final @NotNull Message message) {
    return CompletableFuture.runAsync(
        () -> this.connection.publish(channel, message.toByteArray()),
        this.executorService);
  }

  @Override
  public @NotNull CompletableFuture<Void> subscribe(final @NotNull String channel) {
    return CompletableFuture.runAsync(() -> {
      final var dispatcher = this.connection.createDispatcher();
      dispatcher.subscribe(channel, QUEUE_GROUP, msg -> Result.of(() -> {
        final var message = MessageRegistry.getInstance().unpack(msg.getData());
        this.eventBus.publish(message);

        return null;
      }));

      this.dispatchers.put(channel, dispatcher);
    }, this.executorService);
  }

  @Override
  public @NotNull CompletableFuture<Void> unsubscribe(final @NotNull String channel) {
    return CompletableFuture.runAsync(() -> {
      final var dispatcher = this.dispatchers.remove(channel);

      if (dispatcher == null) {
        return;
      }

      this.connection.closeDispatcher(dispatcher);
    }, this.executorService);
  }

  @Override
  public void close() {
    Result.of(() -> {
      this.connection.close();

      return null;
    });
  }

  @Override
  public @NotNull CompletableFuture<Result<Long, String>> connect(final @NotNull Credentials credentials) {
    final var timer = Timer.start();

    return CompletableFuture.supplyAsync(() -> Result.of(() -> {
      final Collection<String> servers = parseServerUrls(credentials);

      final var options = new Options.Builder()
          .servers(servers.toArray(new String[0]))
          .token(credentials.password().toCharArray())
          .connectionName("celery-nats-cluster")
          .maxReconnects(-1)
          .reconnectWait(java.time.Duration.ofSeconds(2))
          .connectExecutor(this.executorService)
          .build();

      this.connection = Nats.connect(options);

      return timer.end();
    }).mapErr(Exception::getMessage));
  }

  @Override
  public @NotNull CompletableFuture<Boolean> isConnected() {
    return CompletableFuture.supplyAsync(() -> {
      final var status = this.connection.getStatus();

      return status == Status.CONNECTED;
    }, this.executorService);
  }

  /**
   * Parses server URLs from credentials. The IP field may contain a single
   * {@code host:port} or a comma-separated list of {@code host:port} pairs.
   */
  private static List<String> parseServerUrls(final @NotNull Credentials credentials) {
    final String ipField = credentials.ip();

    if (ipField.contains(",")) {
      return java.util.Arrays.stream(ipField.split(","))
          .map(String::strip)
          .map(entry -> {
            if (entry.startsWith("nats://")) {
              return entry;
            }

            return "nats://" + entry;
          })
          .toList();
    }

    final String host;
    final int port;

    if (ipField.contains(":")) {
      final String[] parts = ipField.split(":");
      host = parts[0];
      port = Integer.parseInt(parts[1]);
    } else {
      host = ipField;
      port = credentials.port();
    }

    return List.of("nats://" + host + ":" + port);
  }
}
