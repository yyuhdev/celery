package de.yyuh.celery.api.migration;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface IMigration {

  int version();

  @NotNull
  String description();

  @NotNull
  CompletableFuture<Void> up();

  @NotNull
  CompletableFuture<Void> down();
}
