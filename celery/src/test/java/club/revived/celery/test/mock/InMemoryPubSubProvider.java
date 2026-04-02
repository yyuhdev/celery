package club.revived.celery.test.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import club.revived.celery.database.model.DatabaseCredentials;
import club.revived.celery.messaging.provider.PubSubProvider;

/**
 * In-memory implementation of {@link PubSubProvider} for testing purposes.
 * 
 * <p>This mock simulates NATS pub/sub behavior without requiring an actual NATS server:
 * <ul>
 *   <li>Channel subscriptions with multiple handlers per channel</li>
 *   <li>Message publishing to all subscribers</li>
 *   <li>Message history tracking for test verification</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * InMemoryPubSubProvider pubSub = new InMemoryPubSubProvider();
 * 
 * // Use in tests
 * Celery celery = Celery.builder()
 *     .pubSub(pubSub)
 *     .nodeId("test-node")
 *     .build();
 * 
 * // Verify published messages
 * assertThat(pubSub.getPublishedMessages("my-channel")).hasSize(1);
 * }</pre>
 */
public final class InMemoryPubSubProvider implements PubSubProvider {

  private final Map<String, List<Consumer<byte[]>>> subscriptions = new ConcurrentHashMap<>();
  private final Map<String, List<PublishedMessage>> messageHistory = new ConcurrentHashMap<>();
  
  private boolean connected = false;
  private boolean recordHistory = true;

  @Override
  public void connect(final @NotNull DatabaseCredentials creds) {
    this.connected = true;
  }

  @Override
  @NotNull
  public CompletableFuture<Void> publish(final @NotNull String channel, final byte[] message) {
    return CompletableFuture.runAsync(() -> {
      checkConnection();
      
      // Record message for test verification
      if (recordHistory) {
        messageHistory
            .computeIfAbsent(channel, _ -> new CopyOnWriteArrayList<>())
            .add(new PublishedMessage(channel, message, System.currentTimeMillis()));
      }
      
      // Deliver to all subscribers
      final List<Consumer<byte[]>> handlers = subscriptions.get(channel);
      if (handlers != null) {
        for (final Consumer<byte[]> handler : handlers) {
          try {
            handler.accept(message);
          } catch (final Exception e) {
            // Log but don't fail - similar to real NATS behavior
            System.err.println("Error in subscriber handler for channel " + channel + ": " + e.getMessage());
          }
        }
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> subscribe(
      final @NotNull String channel,
      final @NotNull Consumer<byte[]> handler) {
    return CompletableFuture.runAsync(() -> {
      checkConnection();
      
      subscriptions
          .computeIfAbsent(channel, _ -> new CopyOnWriteArrayList<>())
          .add(handler);
    });
  }

  // ==================== Test Helper Methods ====================

  /**
   * Returns all published messages for a channel (for test verification).
   * 
   * @param channel the channel
   * @return list of published messages
   */
  @NotNull
  public List<PublishedMessage> getPublishedMessages(final @NotNull String channel) {
    return new ArrayList<>(messageHistory.getOrDefault(channel, List.of()));
  }

  /**
   * Returns all published messages across all channels (for test verification).
   * 
   * @return list of all published messages
   */
  @NotNull
  public List<PublishedMessage> getAllPublishedMessages() {
    final List<PublishedMessage> allMessages = new ArrayList<>();
    messageHistory.values().forEach(allMessages::addAll);
    allMessages.sort((a, b) -> Long.compare(a.timestamp(), b.timestamp()));
    return allMessages;
  }

  /**
   * Returns the number of subscribers for a channel (for test verification).
   * 
   * @param channel the channel
   * @return the subscriber count
   */
  public int getSubscriberCount(final @NotNull String channel) {
    final List<Consumer<byte[]>> handlers = subscriptions.get(channel);
    return handlers != null ? handlers.size() : 0;
  }

  /**
   * Returns all channels with active subscriptions (for test verification).
   * 
   * @return list of channel names
   */
  @NotNull
  public List<String> getSubscribedChannels() {
    return new ArrayList<>(subscriptions.keySet());
  }

  /**
   * Clears all subscriptions and message history (for test cleanup).
   */
  public void clear() {
    subscriptions.clear();
    messageHistory.clear();
  }

  /**
   * Clears only the message history, keeping subscriptions (for test isolation).
   */
  public void clearHistory() {
    messageHistory.clear();
  }

  /**
   * Enables or disables message history recording.
   * 
   * @param record true to record, false to skip
   */
  public void setRecordHistory(final boolean record) {
    this.recordHistory = record;
  }

  /**
   * Checks if the provider is connected.
   * 
   * @return true if connected
   */
  public boolean isConnected() {
    return connected;
  }

  /**
   * Simulates disconnection (for testing error scenarios).
   */
  public void disconnect() {
    connected = false;
  }

  /**
   * Simulates an external message arriving on a channel.
   * This is useful for testing message handlers without publishing.
   * 
   * @param channel the channel
   * @param message the message data
   */
  public void simulateIncomingMessage(final @NotNull String channel, final byte[] message) {
    final List<Consumer<byte[]>> handlers = subscriptions.get(channel);
    if (handlers != null) {
      for (final Consumer<byte[]> handler : handlers) {
        handler.accept(message);
      }
    }
  }

  // ==================== Internal Methods ====================

  private void checkConnection() {
    if (!connected) {
      throw new IllegalStateException("InMemoryPubSubProvider is not connected");
    }
  }

  /**
   * Record of a published message for test verification.
   * 
   * @param channel   the channel the message was published to
   * @param data      the message data
   * @param timestamp the time the message was published
   */
  public record PublishedMessage(
      @NotNull String channel,
      byte[] data,
      long timestamp) {
  }
}
