package club.revived.celery.database.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DatabaseCredentials(
    @Nullable String user,
    @NotNull String host,
    @Nullable String password,
    int port,
    @Nullable String database) {
}
