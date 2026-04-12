package de.yyuh.celery.api;

import org.jetbrains.annotations.NotNull;

public interface IDatabaseType {

  @NotNull
  String name();

  int defaultPort();

  @NotNull
  CeleryPlatformType defaultPlatform();
}
