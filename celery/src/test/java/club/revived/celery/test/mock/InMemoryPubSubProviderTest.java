package club.revived.celery.test.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import club.revived.celery.database.model.DatabaseCredentials;

/**
 * Test suite for InMemoryPubSubProvider.
 */
@DisplayName("InMemoryPubSubProvider")
class InMemoryPubSubProviderTest {

  private InMemoryPubSubProvider provider;

  @BeforeEach
  void setUp() {
    provider = new InMemoryPubSubProvider();
    provider.connect(new DatabaseCredentials(null, "localhost", null, 4222, null));
  }

  @Nested
  @DisplayName("Connection")
  class ConnectionTests {

    @Test
    @DisplayName("should connect successfully")
    void shouldConnectSuccessfully() {
      assertThat(provider.isConnected()).isTrue();
    }

    @Test
    @DisplayName("should disconnect")
    void shouldDisconnect() {
      provider.disconnect();

      assertThat(provider.isConnected()).isFalse();
    }

    @Test
    @DisplayName("should throw when not connected")
    void shouldThrowWhenNotConnected() {
      provider.disconnect();

      assertThatThrownBy(() -> provider.publish("channel", "data".getBytes()).join())
          .hasCauseInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("Subscribe")
  class SubscribeTests {

    @Test
    @DisplayName("should subscribe to channel")
    void shouldSubscribeToChannel() {
      provider.subscribe("my-channel", _ -> {}).join();

      assertThat(provider.getSubscriberCount("my-channel")).isEqualTo(1);
    }

    @Test
    @DisplayName("should allow multiple subscribers to same channel")
    void shouldAllowMultipleSubscribers() {
      provider.subscribe("channel", _ -> {}).join();
      provider.subscribe("channel", _ -> {}).join();
      provider.subscribe("channel", _ -> {}).join();

      assertThat(provider.getSubscriberCount("channel")).isEqualTo(3);
    }

    @Test
    @DisplayName("should return subscribed channels")
    void shouldReturnSubscribedChannels() {
      provider.subscribe("channel1", _ -> {}).join();
      provider.subscribe("channel2", _ -> {}).join();

      final List<String> channels = provider.getSubscribedChannels();

      assertThat(channels).containsExactlyInAnyOrder("channel1", "channel2");
    }
  }

  @Nested
  @DisplayName("Publish")
  class PublishTests {

    @Test
    @DisplayName("should publish message")
    void shouldPublishMessage() {
      final byte[] message = "test-message".getBytes();

      provider.publish("channel", message).join();

      final List<InMemoryPubSubProvider.PublishedMessage> messages = provider.getPublishedMessages("channel");
      assertThat(messages).hasSize(1);
      assertThat(messages.getFirst().data()).isEqualTo(message);
    }

    @Test
    @DisplayName("should deliver message to subscriber")
    void shouldDeliverMessageToSubscriber() throws InterruptedException {
      final AtomicReference<byte[]> received = new AtomicReference<>();
      final CountDownLatch latch = new CountDownLatch(1);

      provider.subscribe("channel", data -> {
        received.set(data);
        latch.countDown();
      }).join();

      provider.publish("channel", "hello".getBytes()).join();

      assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
      assertThat(new String(received.get())).isEqualTo("hello");
    }

    @Test
    @DisplayName("should deliver message to multiple subscribers")
    void shouldDeliverToMultipleSubscribers() throws InterruptedException {
      final CountDownLatch latch = new CountDownLatch(3);

      provider.subscribe("channel", _ -> latch.countDown()).join();
      provider.subscribe("channel", _ -> latch.countDown()).join();
      provider.subscribe("channel", _ -> latch.countDown()).join();

      provider.publish("channel", "message".getBytes()).join();

      assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("should not deliver to different channels")
    void shouldNotDeliverToDifferentChannels() throws InterruptedException {
      final CountDownLatch latch = new CountDownLatch(1);

      provider.subscribe("channel1", _ -> latch.countDown()).join();

      provider.publish("channel2", "message".getBytes()).join();

      // Wait a bit to ensure no message is delivered
      assertThat(latch.await(100, TimeUnit.MILLISECONDS)).isFalse();
    }

    @Test
    @DisplayName("should record message history")
    void shouldRecordMessageHistory() {
      provider.publish("ch1", "msg1".getBytes()).join();
      provider.publish("ch1", "msg2".getBytes()).join();
      provider.publish("ch2", "msg3".getBytes()).join();

      assertThat(provider.getPublishedMessages("ch1")).hasSize(2);
      assertThat(provider.getPublishedMessages("ch2")).hasSize(1);
      assertThat(provider.getAllPublishedMessages()).hasSize(3);
    }
  }

  @Nested
  @DisplayName("Test Helpers")
  class TestHelperTests {

    @Test
    @DisplayName("should clear all data")
    void shouldClearAllData() {
      provider.subscribe("channel", _ -> {}).join();
      provider.publish("channel", "message".getBytes()).join();

      provider.clear();

      assertThat(provider.getSubscribedChannels()).isEmpty();
      assertThat(provider.getAllPublishedMessages()).isEmpty();
    }

    @Test
    @DisplayName("should clear only history")
    void shouldClearOnlyHistory() {
      provider.subscribe("channel", _ -> {}).join();
      provider.publish("channel", "message".getBytes()).join();

      provider.clearHistory();

      assertThat(provider.getSubscriberCount("channel")).isEqualTo(1);
      assertThat(provider.getAllPublishedMessages()).isEmpty();
    }

    @Test
    @DisplayName("should disable history recording")
    void shouldDisableHistoryRecording() {
      provider.setRecordHistory(false);

      provider.publish("channel", "message".getBytes()).join();

      assertThat(provider.getAllPublishedMessages()).isEmpty();
    }

    @Test
    @DisplayName("should simulate incoming message")
    void shouldSimulateIncomingMessage() throws InterruptedException {
      final AtomicReference<byte[]> received = new AtomicReference<>();
      final CountDownLatch latch = new CountDownLatch(1);

      provider.subscribe("channel", data -> {
        received.set(data);
        latch.countDown();
      }).join();

      provider.simulateIncomingMessage("channel", "external-message".getBytes());

      assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
      assertThat(new String(received.get())).isEqualTo("external-message");
    }
  }

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandlingTests {

    @Test
    @DisplayName("should handle subscriber exception gracefully")
    void shouldHandleSubscriberExceptionGracefully() throws InterruptedException {
      final AtomicReference<byte[]> received = new AtomicReference<>();
      final CountDownLatch latch = new CountDownLatch(1);

      // First subscriber throws exception
      provider.subscribe("channel", _ -> {
        throw new RuntimeException("Test exception");
      }).join();

      // Second subscriber should still receive message
      provider.subscribe("channel", data -> {
        received.set(data);
        latch.countDown();
      }).join();

      provider.publish("channel", "message".getBytes()).join();

      assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
      assertThat(new String(received.get())).isEqualTo("message");
    }
  }
}
