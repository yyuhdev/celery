package club.revived.celery;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import club.revived.celery.database.model.DatabaseCredentials;

public record Credentials(
    @Nullable String user,
    @NotNull String host,
    @Nullable String password,
    int port,
    @Nullable String database) {

  @NotNull
  public static Credentials forMongo(
      final @NotNull String user,
      final @NotNull String host,
      final @Nullable String password,
      final int port,
      final @NotNull String database) {
    return new Credentials(user, host, password, port, database);
  }

  @NotNull
  public static Credentials forInflux(
      final @NotNull String org,
      final @NotNull String host,
      final @NotNull String token,
      final int port,
      final @NotNull String bucket) {
    return new Credentials(org, host, token, port, bucket);
  }

  @NotNull
  public static Credentials forRedis(
      final @NotNull String host,
      final @Nullable String password,
      final int port) {
    return new Credentials(null, host, password, port, null);
  }

  @NotNull
  public static Credentials forRedis(
      final @NotNull String host,
      final @Nullable String password,
      final int port,
      final @Nullable String database) {
    return new Credentials(null, host, password, port, database);
  }

  @NotNull
  public static Credentials forNats(
      final @NotNull String host,
      final @Nullable String password,
      final int port) {
    return new Credentials(null, host, password, port, null);
  }

  @NotNull
  public static Credentials forNats(
      final @NotNull String host,
      final @Nullable String password,
      final int port,
      final @Nullable String namespace) {
    return new Credentials(null, host, password, port, namespace);
  }

  @NotNull
  public DatabaseCredentials toDatabaseCredentials() {
    return new DatabaseCredentials(
        user,
        host,
        password,
        port,
        database);
  }
}
