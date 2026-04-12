package de.yyuh.celery.api.credentials;

public record Credentials(
    String user,
    String password,
    String ip,
    int port) {
}
