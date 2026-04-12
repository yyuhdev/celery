package club.revived.celery.db;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import club.revived.celery.credentials.DatabaseType;

public final class DatabaseRegistry {

  private final Map<DatabaseType, IDatabaseProvider> providers = new ConcurrentHashMap<>();

  private void register() {

  }

}
