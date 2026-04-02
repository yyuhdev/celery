package club.revived.celery.test.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import club.revived.celery.database.model.DatabaseCredentials;
import club.revived.celery.database.model.query.QueryFilter;
import club.revived.celery.database.model.query.QueryFilterBuilder;
import club.revived.celery.test.model.TestUser;

/**
 * Test suite for InMemoryDatabaseProvider.
 */
@DisplayName("InMemoryDatabaseProvider")
class InMemoryDatabaseProviderTest {

  private InMemoryDatabaseProvider<TestUser> provider;

  @BeforeEach
  void setUp() {
    provider = new InMemoryDatabaseProvider<>();
    provider.connect(new DatabaseCredentials("user", "localhost", "pass", 27017, "test"));
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

      assertThatThrownBy(() -> provider.findAll().join())
          .hasCauseInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("Write Operations")
  class WriteOperationTests {

    @Test
    @DisplayName("should write single entity")
    void shouldWriteSingleEntity() {
      final TestUser user = TestUser.create("john", "john@example.com", 25);

      provider.write(user).join();

      assertThat(provider.size()).isEqualTo(1);
      assertThat(provider.containsId(user.id())).isTrue();
    }

    @Test
    @DisplayName("should write batch of entities")
    void shouldWriteBatchOfEntities() {
      final List<TestUser> users = List.of(
          TestUser.create("alice", "alice@example.com", 30),
          TestUser.create("bob", "bob@example.com", 35),
          TestUser.create("charlie", "charlie@example.com", 40));

      provider.writeBatch(users).join();

      assertThat(provider.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("should update existing entity")
    void shouldUpdateExistingEntity() {
      final UUID id = UUID.randomUUID();
      final TestUser original = TestUser.create(id, "john", "john@example.com", 25);
      final TestUser updated = TestUser.create(id, "john_updated", "john@example.com", 26);

      provider.write(original).join();
      provider.write(updated).join();

      assertThat(provider.size()).isEqualTo(1);
      assertThat(provider.getById(id).username()).isEqualTo("john_updated");
    }
  }

  @Nested
  @DisplayName("Find Operations")
  class FindOperationTests {

    @BeforeEach
    void setUpData() {
      provider.addDirect(TestUser.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), "alice", "alice@example.com", 25));
      provider.addDirect(TestUser.create(UUID.fromString("00000000-0000-0000-0000-000000000002"), "bob", "bob@example.com", 30));
      provider.addDirect(TestUser.create(UUID.fromString("00000000-0000-0000-0000-000000000003"), "charlie", "charlie@example.com", 35));
      provider.addDirect(TestUser.create(UUID.fromString("00000000-0000-0000-0000-000000000004"), "diana", "diana@example.com", 40));
      provider.addDirect(new TestUser(UUID.fromString("00000000-0000-0000-0000-000000000005"), "eve", "eve@example.com", 45, false));
    }

    @Test
    @DisplayName("should find all entities")
    void shouldFindAllEntities() {
      final List<TestUser> users = provider.findAll().join();

      assertThat(users).hasSize(5);
    }

    @Test
    @DisplayName("should find by equality filter")
    void shouldFindByEqualityFilter() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .eq("user_name", "bob")
          .build();

      final Optional<TestUser> result = provider.find(filter).join();

      assertThat(result).isPresent();
      assertThat(result.get().username()).isEqualTo("bob");
    }

    @Test
    @DisplayName("should find by greater than filter")
    void shouldFindByGreaterThanFilter() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .gt("age", 35)
          .build();

      final List<TestUser> results = provider.findAll(filter).join();

      assertThat(results).hasSize(2);
      assertThat(results).allMatch(u -> u.age() > 35);
    }

    @Test
    @DisplayName("should find by less than filter")
    void shouldFindByLessThanFilter() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .lt("age", 30)
          .build();

      final List<TestUser> results = provider.findAll(filter).join();

      assertThat(results).hasSize(1);
      assertThat(results.getFirst().username()).isEqualTo("alice");
    }

    @Test
    @DisplayName("should find by IN filter")
    void shouldFindByInFilter() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .in("user_name", List.of("alice", "charlie", "eve"))
          .build();

      final List<TestUser> results = provider.findAll(filter).join();

      assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("should find by contains filter")
    void shouldFindByContainsFilter() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .contains("email", "example")
          .build();

      final List<TestUser> results = provider.findAll(filter).join();

      assertThat(results).hasSize(5);
    }

    @Test
    @DisplayName("should find by starts with filter")
    void shouldFindByStartsWithFilter() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .startsWith("user_name", "c")
          .build();

      final List<TestUser> results = provider.findAll(filter).join();

      assertThat(results).hasSize(1);
      assertThat(results.getFirst().username()).isEqualTo("charlie");
    }

    @Test
    @DisplayName("should find by boolean filter")
    void shouldFindByBooleanFilter() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .eq("active", false)
          .build();

      final List<TestUser> results = provider.findAll(filter).join();

      assertThat(results).hasSize(1);
      assertThat(results.getFirst().username()).isEqualTo("eve");
    }

    @Test
    @DisplayName("should find with multiple conditions")
    void shouldFindWithMultipleConditions() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .gte("age", 30)
          .lte("age", 40)
          .eq("active", true)
          .build();

      final List<TestUser> results = provider.findAll(filter).join();

      assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("should return empty for no matches")
    void shouldReturnEmptyForNoMatches() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .eq("user_name", "nonexistent")
          .build();

      final Optional<TestUser> result = provider.find(filter).join();

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Sorting and Limiting")
  class SortingAndLimitingTests {

    @BeforeEach
    void setUpData() {
      provider.addDirect(TestUser.create(UUID.randomUUID(), "charlie", "c@example.com", 35));
      provider.addDirect(TestUser.create(UUID.randomUUID(), "alice", "a@example.com", 25));
      provider.addDirect(TestUser.create(UUID.randomUUID(), "bob", "b@example.com", 30));
      provider.addDirect(TestUser.create(UUID.randomUUID(), "diana", "d@example.com", 40));
    }

    @Test
    @DisplayName("should sort ascending")
    void shouldSortAscending() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .sortAsc("age")
          .build();

      final List<TestUser> results = provider.findAll(filter).join();

      assertThat(results.get(0).age()).isEqualTo(25);
      assertThat(results.get(1).age()).isEqualTo(30);
      assertThat(results.get(2).age()).isEqualTo(35);
      assertThat(results.get(3).age()).isEqualTo(40);
    }

    @Test
    @DisplayName("should sort descending")
    void shouldSortDescending() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .sortDesc("age")
          .build();

      final List<TestUser> results = provider.findAll(filter).join();

      assertThat(results.get(0).age()).isEqualTo(40);
      assertThat(results.get(3).age()).isEqualTo(25);
    }

    @Test
    @DisplayName("should limit results")
    void shouldLimitResults() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .limit(2)
          .build();

      final List<TestUser> results = provider.findAll(filter).join();

      assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("should sort and limit together")
    void shouldSortAndLimitTogether() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .sortDesc("age")
          .limit(2)
          .build();

      final List<TestUser> results = provider.findAll(filter).join();

      assertThat(results).hasSize(2);
      assertThat(results.get(0).age()).isEqualTo(40);
      assertThat(results.get(1).age()).isEqualTo(35);
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperationTests {

    @BeforeEach
    void setUpData() {
      provider.addDirect(TestUser.create(UUID.randomUUID(), "alice", "alice@example.com", 25));
      provider.addDirect(TestUser.create(UUID.randomUUID(), "bob", "bob@example.com", 30));
      provider.addDirect(TestUser.create(UUID.randomUUID(), "charlie", "charlie@example.com", 35));
    }

    @Test
    @DisplayName("should delete single entity")
    void shouldDeleteSingleEntity() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .eq("user_name", "bob")
          .build();

      provider.delete(filter).join();

      assertThat(provider.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("should delete multiple entities")
    void shouldDeleteMultipleEntities() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .gte("age", 30)
          .build();

      provider.delete(filter).join();

      assertThat(provider.size()).isEqualTo(1);
      assertThat(provider.getAllEntities().getFirst().username()).isEqualTo("alice");
    }

    @Test
    @DisplayName("should handle delete with no matches")
    void shouldHandleDeleteWithNoMatches() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .eq("user_name", "nonexistent")
          .build();

      provider.delete(filter).join();

      assertThat(provider.size()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("Batch Find Operations")
  class BatchFindOperationTests {

    @BeforeEach
    void setUpData() {
      provider.addDirect(TestUser.create(UUID.randomUUID(), "alice", "alice@example.com", 25));
      provider.addDirect(TestUser.create(UUID.randomUUID(), "bob", "bob@example.com", 30));
      provider.addDirect(TestUser.create(UUID.randomUUID(), "charlie", "charlie@example.com", 35));
    }

    @Test
    @DisplayName("should find batch with multiple filters")
    void shouldFindBatchWithMultipleFilters() {
      final List<QueryFilter<TestUser>> filters = List.of(
          new QueryFilterBuilder<>(TestUser.class).eq("user_name", "alice").build(),
          new QueryFilterBuilder<>(TestUser.class).eq("user_name", "charlie").build());

      final List<TestUser> results = provider.findBatch(filters).join();

      assertThat(results).hasSize(2);
    }
  }

  @Nested
  @DisplayName("Test Helpers")
  class TestHelperTests {

    @Test
    @DisplayName("should clear all data")
    void shouldClearAllData() {
      provider.addDirect(TestUser.create("alice", "alice@example.com", 25));
      provider.addDirect(TestUser.create("bob", "bob@example.com", 30));

      provider.clear();

      assertThat(provider.size()).isZero();
    }

    @Test
    @DisplayName("should add entity directly")
    void shouldAddEntityDirectly() {
      final TestUser user = TestUser.create("alice", "alice@example.com", 25);

      provider.addDirect(user);

      assertThat(provider.getById(user.id())).isEqualTo(user);
    }
  }
}
