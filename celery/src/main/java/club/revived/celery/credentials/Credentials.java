package club.revived.celery.credentials;

public record Credentials(
    String user,
    String password,
    String ip,
    int port) {
}
