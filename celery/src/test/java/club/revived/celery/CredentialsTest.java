package club.revived.celery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test suite for the Credentials record.
 */
@DisplayName("Credentials")
class CredentialsTest {

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create MongoDB credentials")
    void shouldCreateMongoCredentials() {
      final Credentials creds = Credentials.forMongo("admin", "cluster.mongodb.net", "secret", 27017, "mydb");

      assertThat(creds.user()).isEqualTo("admin");
      assertThat(creds.host()).isEqualTo("cluster.mongodb.net");
      assertThat(creds.password()).isEqualTo("secret");
      assertThat(creds.port()).isEqualTo(27017);
      assertThat(creds.database()).isEqualTo("mydb");
    }

    @Test
    @DisplayName("should create MongoDB credentials with null password")
    void shouldCreateMongoCredentialsWithNullPassword() {
      final Credentials creds = Credentials.forMongo("admin", "localhost", null, 27017, "testdb");

      assertThat(creds.user()).isEqualTo("admin");
      assertThat(creds.password()).isNull();
    }

    @Test
    @DisplayName("should create InfluxDB credentials")
    void shouldCreateInfluxCredentials() {
      final Credentials creds = Credentials.forInflux("my-org", "influx.example.com", "api-token", 8086, "metrics");

      assertThat(creds.user()).isEqualTo("my-org");
      assertThat(creds.host()).isEqualTo("influx.example.com");
      assertThat(creds.password()).isEqualTo("api-token");
      assertThat(creds.port()).isEqualTo(8086);
      assertThat(creds.database()).isEqualTo("metrics");
    }

    @Test
    @DisplayName("should create Redis credentials")
    void shouldCreateRedisCredentials() {
      final Credentials creds = Credentials.forRedis("redis.example.com", "redis-pass", 6379);

      assertThat(creds.user()).isNull();
      assertThat(creds.host()).isEqualTo("redis.example.com");
      assertThat(creds.password()).isEqualTo("redis-pass");
      assertThat(creds.port()).isEqualTo(6379);
      assertThat(creds.database()).isNull();
    }

    @Test
    @DisplayName("should create Redis credentials with database")
    void shouldCreateRedisCredentialsWithDatabase() {
      final Credentials creds = Credentials.forRedis("localhost", null, 6379, "0");

      assertThat(creds.host()).isEqualTo("localhost");
      assertThat(creds.password()).isNull();
      assertThat(creds.database()).isEqualTo("0");
    }

    @Test
    @DisplayName("should create NATS credentials")
    void shouldCreateNatsCredentials() {
      final Credentials creds = Credentials.forNats("nats.example.com", "nats-token", 4222);

      assertThat(creds.user()).isNull();
      assertThat(creds.host()).isEqualTo("nats.example.com");
      assertThat(creds.password()).isEqualTo("nats-token");
      assertThat(creds.port()).isEqualTo(4222);
      assertThat(creds.database()).isNull();
    }

    @Test
    @DisplayName("should create NATS credentials with namespace")
    void shouldCreateNatsCredentialsWithNamespace() {
      final Credentials creds = Credentials.forNats("localhost", "token", 4222, "my-namespace");

      assertThat(creds.database()).isEqualTo("my-namespace");
    }
  }

  @Nested
  @DisplayName("Conversions")
  class ConversionTests {

    @Test
    @DisplayName("should convert to database credentials")
    void shouldConvertToDatabaseCredentials() {
      final Credentials creds = Credentials.forMongo("user", "host", "pass", 27017, "db");

      final club.revived.celery.database.model.DatabaseCredentials dbCreds = creds.toDatabaseCredentials();

      assertThat(dbCreds.user()).isEqualTo("user");
      assertThat(dbCreds.host()).isEqualTo("host");
      assertThat(dbCreds.password()).isEqualTo("pass");
      assertThat(dbCreds.port()).isEqualTo(27017);
      assertThat(dbCreds.database()).isEqualTo("db");
    }

    @Test
    @DisplayName("should convert to messaging credentials")
    void shouldConvertToMessagingCredentials() {
      final Credentials creds = Credentials.forRedis("localhost", "secret", 6379, "0");

      final var msgCreds = creds.toDatabaseCredentials();

      assertThat(msgCreds.host()).isEqualTo("localhost");
      assertThat(msgCreds.password()).isEqualTo("secret");
      assertThat(msgCreds.port()).isEqualTo(6379);
      assertThat(msgCreds.database()).isEqualTo("0");
    }
  }

  @Nested
  @DisplayName("Record Equality")
  class EqualityTests {

    @Test
    @DisplayName("should be equal for same values")
    void shouldBeEqualForSameValues() {
      final Credentials creds1 = Credentials.forMongo("user", "host", "pass", 27017, "db");
      final Credentials creds2 = Credentials.forMongo("user", "host", "pass", 27017, "db");

      assertThat(creds1).isEqualTo(creds2);
      assertThat(creds1.hashCode()).isEqualTo(creds2.hashCode());
    }

    @Test
    @DisplayName("should not be equal for different values")
    void shouldNotBeEqualForDifferentValues() {
      final Credentials creds1 = Credentials.forMongo("user1", "host", "pass", 27017, "db");
      final Credentials creds2 = Credentials.forMongo("user2", "host", "pass", 27017, "db");

      assertThat(creds1).isNotEqualTo(creds2);
    }
  }
}
