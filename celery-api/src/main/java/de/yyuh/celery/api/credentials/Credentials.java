package de.yyuh.celery.api.credentials;

/**
 * Holds database connection credentials.
 *
 * <p>
 * This record contains the authentication and connection information
 * required to connect to a database, including username, password,
 * hostname, and port.
 *
 * @param user     the database username
 * @param password the database password
 * @param ip       the database hostname or IP address
 * @param port     the database port number
 */
public record Credentials(
    String user,
    String password,
    String ip,
    String database,
    int port,

    // InfluxDB & S3
    String bucket,

    // S3 only
    String accessKey,
    String accessKeyId,
    String region) {

}
