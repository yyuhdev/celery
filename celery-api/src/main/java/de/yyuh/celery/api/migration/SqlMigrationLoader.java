package de.yyuh.celery.api.migration;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class SqlMigrationLoader {

    private static final Pattern FILENAME_PATTERN = Pattern.compile("V(\\d+)__(.+)\\.(up|down)\\.sql");

    private final MigrationManager migrationManager;
    private final Function<String, CompletableFuture<Void>> sqlExecutor;

    public SqlMigrationLoader(@NotNull MigrationManager migrationManager, @NotNull Function<String, CompletableFuture<Void>> sqlExecutor) {
        this.migrationManager = migrationManager;
        this.sqlExecutor = sqlExecutor;
    }

    public void loadFromDirectory(@NotNull Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Path is not a directory: " + directory);
        }

        try (Stream<Path> files = Files.list(directory)) {
            files.filter(f -> f.toString().endsWith(".up.sql"))
                    .forEach(this::processUpFile);
        }
    }

    private void processUpFile(Path upFile) {
        String filename = upFile.getFileName().toString();
        Matcher matcher = FILENAME_PATTERN.matcher(filename);

        if (matcher.matches()) {
            int version = Integer.parseInt(matcher.group(1));
            String description = matcher.group(2).replace("_", " ");
            Path downFile = upFile.resolveSibling(filename.replace(".up.sql", ".down.sql"));

            if (Files.exists(downFile)) {
                migrationManager.register(new FileSqlMigration(version, description, upFile, downFile, sqlExecutor));
            } else {
                // Register without down migration or throw error? 
                // For now, let's require both for consistency with IMigration.
                throw new RuntimeException("Missing down migration for version " + version + ": " + downFile);
            }
        }
    }
}
