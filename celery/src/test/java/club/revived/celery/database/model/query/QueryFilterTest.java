package club.revived.celery.database.model.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import club.revived.celery.test.model.TestUser;

/**
 * Test suite for QueryFilter and QueryFilterBuilder.
 */
@DisplayName("QueryFilter")
class QueryFilterTest {

  @Nested
  @DisplayName("QueryFilterBuilder")
  class QueryFilterBuilderTests {

    @Test
    @DisplayName("should build empty filter")
    void shouldBuildEmptyFilter() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class).build();

      assertThat(filter.type()).isEqualTo(TestUser.class);
      assertThat(filter.conditions()).isEmpty();
      assertThat(filter.time()).isNull();
      assertThat(filter.limit()).isNull();
      assertThat(filter.sort()).isNull();
    }

    @Test
    @DisplayName("should build filter with eq condition")
    void shouldBuildFilterWithEqCondition() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .eq("username", "john")
          .build();

      assertThat(filter.conditions()).hasSize(1);
      assertThat(filter.conditions().getFirst().field()).isEqualTo("username");
      assertThat(filter.conditions().getFirst().operator()).isEqualTo(QueryFilter.Operator.EQ);
      assertThat(filter.conditions().getFirst().value()).isEqualTo("john");
    }

    @Test
    @DisplayName("should build filter with ne condition")
    void shouldBuildFilterWithNeCondition() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .ne("status", "inactive")
          .build();

      assertThat(filter.conditions().getFirst().operator()).isEqualTo(QueryFilter.Operator.NE);
    }

    @Test
    @DisplayName("should build filter with gt condition")
    void shouldBuildFilterWithGtCondition() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .gt("age", 25)
          .build();

      assertThat(filter.conditions().getFirst().operator()).isEqualTo(QueryFilter.Operator.GT);
      assertThat(filter.conditions().getFirst().value()).isEqualTo(25);
    }

    @Test
    @DisplayName("should build filter with gte condition")
    void shouldBuildFilterWithGteCondition() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .gte("age", 18)
          .build();

      assertThat(filter.conditions().getFirst().operator()).isEqualTo(QueryFilter.Operator.GTE);
    }

    @Test
    @DisplayName("should build filter with lt condition")
    void shouldBuildFilterWithLtCondition() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .lt("age", 65)
          .build();

      assertThat(filter.conditions().getFirst().operator()).isEqualTo(QueryFilter.Operator.LT);
    }

    @Test
    @DisplayName("should build filter with lte condition")
    void shouldBuildFilterWithLteCondition() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .lte("score", 100)
          .build();

      assertThat(filter.conditions().getFirst().operator()).isEqualTo(QueryFilter.Operator.LTE);
    }

    @Test
    @DisplayName("should build filter with in condition")
    void shouldBuildFilterWithInCondition() {
      final List<String> values = List.of("admin", "moderator", "user");

      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .in("role", values)
          .build();

      assertThat(filter.conditions().getFirst().operator()).isEqualTo(QueryFilter.Operator.IN);
      assertThat(filter.conditions().getFirst().value()).isEqualTo(values);
    }

    @Test
    @DisplayName("should build filter with contains condition")
    void shouldBuildFilterWithContainsCondition() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .contains("email", "@example.com")
          .build();

      assertThat(filter.conditions().getFirst().operator()).isEqualTo(QueryFilter.Operator.CONTAINS);
    }

    @Test
    @DisplayName("should build filter with startsWith condition")
    void shouldBuildFilterWithStartsWithCondition() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .startsWith("username", "admin_")
          .build();

      assertThat(filter.conditions().getFirst().operator()).isEqualTo(QueryFilter.Operator.STARTS_WITH);
    }

    @Test
    @DisplayName("should build filter with endsWith condition")
    void shouldBuildFilterWithEndsWithCondition() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .endsWith("email", ".org")
          .build();

      assertThat(filter.conditions().getFirst().operator()).isEqualTo(QueryFilter.Operator.ENDS_WITH);
    }

    @Test
    @DisplayName("should build filter with exists condition")
    void shouldBuildFilterWithExistsCondition() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .exists("avatar")
          .build();

      assertThat(filter.conditions().getFirst().operator()).isEqualTo(QueryFilter.Operator.EXISTS);
    }

    @Test
    @DisplayName("should build filter with notExists condition")
    void shouldBuildFilterWithNotExistsCondition() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .notExists("deletedAt")
          .build();

      assertThat(filter.conditions().getFirst().operator()).isEqualTo(QueryFilter.Operator.NOT_EXISTS);
    }

    @Test
    @DisplayName("should build filter with multiple conditions")
    void shouldBuildFilterWithMultipleConditions() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .eq("active", true)
          .gte("age", 18)
          .lt("age", 65)
          .contains("email", "@")
          .build();

      assertThat(filter.conditions()).hasSize(4);
    }

    @Test
    @DisplayName("should build filter with time range")
    void shouldBuildFilterWithTimeRange() {
      final Instant from = Instant.parse("2024-01-01T00:00:00Z");
      final Instant to = Instant.parse("2024-12-31T23:59:59Z");

      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .between(from, to)
          .build();

      assertThat(filter.time()).isNotNull();
      assertThat(filter.time().from()).isEqualTo(from);
      assertThat(filter.time().to()).isEqualTo(to);
    }

    @Test
    @DisplayName("should build filter with limit")
    void shouldBuildFilterWithLimit() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .limit(10)
          .build();

      assertThat(filter.limit()).isEqualTo(10);
    }

    @Test
    @DisplayName("should build filter with ascending sort")
    void shouldBuildFilterWithAscendingSort() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .sortAsc("createdAt")
          .build();

      assertThat(filter.sort()).isNotNull();
      assertThat(filter.sort().field()).isEqualTo("createdAt");
      assertThat(filter.sort().direction()).isEqualTo(QueryFilter.Direction.ASCENDING);
    }

    @Test
    @DisplayName("should build filter with descending sort")
    void shouldBuildFilterWithDescendingSort() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .sortDesc("updatedAt")
          .build();

      assertThat(filter.sort()).isNotNull();
      assertThat(filter.sort().field()).isEqualTo("updatedAt");
      assertThat(filter.sort().direction()).isEqualTo(QueryFilter.Direction.DESCENDING);
    }

    @Test
    @DisplayName("should build filter with generic where clause")
    void shouldBuildFilterWithGenericWhere() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .where("field", QueryFilter.Operator.EQ, "value")
          .build();

      assertThat(filter.conditions()).hasSize(1);
      assertThat(filter.conditions().getFirst().field()).isEqualTo("field");
    }

    @Test
    @DisplayName("should build complete filter with all options")
    void shouldBuildCompleteFilter() {
      final Instant from = Instant.now().minusSeconds(3600);
      final Instant to = Instant.now();

      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .eq("active", true)
          .gte("age", 18)
          .in("role", List.of("user", "admin"))
          .between(from, to)
          .sortDesc("createdAt")
          .limit(50)
          .build();

      assertThat(filter.type()).isEqualTo(TestUser.class);
      assertThat(filter.conditions()).hasSize(3);
      assertThat(filter.time()).isNotNull();
      assertThat(filter.sort()).isNotNull();
      assertThat(filter.limit()).isEqualTo(50);
    }
  }

  @Nested
  @DisplayName("QueryFilter Record")
  class QueryFilterRecordTests {

    @Test
    @DisplayName("should create with all parameters")
    void shouldCreateWithAllParameters() {
      final List<QueryFilter.Condition> conditions = List.of(
          new QueryFilter.Condition("field", QueryFilter.Operator.EQ, "value"));
      final QueryFilter.TimeRange time = new QueryFilter.TimeRange(Instant.now(), Instant.now());
      final QueryFilter.Sort sort = new QueryFilter.Sort("field", QueryFilter.Direction.ASCENDING);

      final QueryFilter<TestUser> filter = new QueryFilter<>(
          TestUser.class, conditions, time, 10, sort);

      assertThat(filter.type()).isEqualTo(TestUser.class);
      assertThat(filter.conditions()).isEqualTo(conditions);
      assertThat(filter.time()).isEqualTo(time);
      assertThat(filter.limit()).isEqualTo(10);
      assertThat(filter.sort()).isEqualTo(sort);
    }

    @Test
    @DisplayName("conditions should be immutable when built")
    void conditionsShouldBeImmutable() {
      final QueryFilter<TestUser> filter = new QueryFilterBuilder<>(TestUser.class)
          .eq("field", "value")
          .build();

      assertThat(filter.conditions()).isUnmodifiable();
    }
  }

  @Nested
  @DisplayName("Condition Record")
  class ConditionRecordTests {

    @Test
    @DisplayName("should create condition")
    void shouldCreateCondition() {
      final QueryFilter.Condition condition = new QueryFilter.Condition(
          "username", QueryFilter.Operator.EQ, "john");

      assertThat(condition.field()).isEqualTo("username");
      assertThat(condition.operator()).isEqualTo(QueryFilter.Operator.EQ);
      assertThat(condition.value()).isEqualTo("john");
    }

    @Test
    @DisplayName("should support equality")
    void shouldSupportEquality() {
      final QueryFilter.Condition c1 = new QueryFilter.Condition("f", QueryFilter.Operator.EQ, "v");
      final QueryFilter.Condition c2 = new QueryFilter.Condition("f", QueryFilter.Operator.EQ, "v");

      assertThat(c1).isEqualTo(c2);
      assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
    }
  }

  @Nested
  @DisplayName("Operator Enum")
  class OperatorEnumTests {

    @Test
    @DisplayName("should have all operators")
    void shouldHaveAllOperators() {
      assertThat(QueryFilter.Operator.values()).containsExactly(
          QueryFilter.Operator.EQ,
          QueryFilter.Operator.NE,
          QueryFilter.Operator.GT,
          QueryFilter.Operator.GTE,
          QueryFilter.Operator.LT,
          QueryFilter.Operator.LTE,
          QueryFilter.Operator.IN,
          QueryFilter.Operator.CONTAINS,
          QueryFilter.Operator.STARTS_WITH,
          QueryFilter.Operator.ENDS_WITH,
          QueryFilter.Operator.EXISTS,
          QueryFilter.Operator.NOT_EXISTS);
    }
  }

  @Nested
  @DisplayName("Direction Enum")
  class DirectionEnumTests {

    @Test
    @DisplayName("should have both directions")
    void shouldHaveBothDirections() {
      assertThat(QueryFilter.Direction.values()).containsExactly(
          QueryFilter.Direction.ASCENDING,
          QueryFilter.Direction.DESCENDING);
    }
  }
}
