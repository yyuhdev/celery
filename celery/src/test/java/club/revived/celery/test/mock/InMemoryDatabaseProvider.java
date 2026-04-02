package club.revived.celery.test.mock;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import club.revived.celery.database.model.DatabaseCredentials;
import club.revived.celery.database.model.annotation.Identifier;
import club.revived.celery.database.model.annotation.Property;
import club.revived.celery.database.model.query.QueryFilter;
import club.revived.celery.database.provider.DatabaseProvider;

/**
 * In-memory implementation of {@link DatabaseProvider} for testing purposes.
 * 
 * <p>This mock simulates MongoDB behavior without requiring an actual MongoDB instance:
 * <ul>
 *   <li>CRUD operations on entities</li>
 *   <li>Query filtering with operators (EQ, NE, GT, LT, etc.)</li>
 *   <li>Sorting and limiting results</li>
 *   <li>Support for @Identifier and @Property annotations</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * InMemoryDatabaseProvider<MyEntity> provider = new InMemoryDatabaseProvider<>();
 * 
 * // Use with Celery
 * Celery celery = Celery.builder()
 *     .database(MyEntity.class, provider, credentials)
 *     .nodeId("test-node")
 *     .build();
 * 
 * // Verify stored data
 * assertThat(provider.getAllEntities()).hasSize(2);
 * }</pre>
 */
public final class InMemoryDatabaseProvider<T> implements DatabaseProvider<T> {

  private final List<T> entities = new CopyOnWriteArrayList<>();
  private final Map<Object, T> entityIndex = new ConcurrentHashMap<>();
  
  private boolean connected = false;

  @Override
  public void connect(final @NotNull DatabaseCredentials credentials) {
    this.connected = true;
  }

  @Override
  @NotNull
  public CompletableFuture<Void> write(final @NotNull T entity) {
    return CompletableFuture.runAsync(() -> {
      checkConnection();
      
      final Object id = extractId(entity);
      
      if (id != null && entityIndex.containsKey(id)) {
        // Update existing entity
        entities.removeIf(e -> id.equals(extractId(e)));
        entityIndex.remove(id);
      }
      
      entities.add(entity);
      if (id != null) {
        entityIndex.put(id, entity);
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> writeBatch(final @NotNull List<T> batch) {
    return CompletableFuture.runAsync(() -> {
      checkConnection();
      
      for (final T entity : batch) {
        final Object id = extractId(entity);
        
        if (id != null && entityIndex.containsKey(id)) {
          entities.removeIf(e -> id.equals(extractId(e)));
          entityIndex.remove(id);
        }
        
        entities.add(entity);
        if (id != null) {
          entityIndex.put(id, entity);
        }
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<List<T>> findAll() {
    return CompletableFuture.supplyAsync(() -> {
      checkConnection();
      return new ArrayList<>(entities);
    });
  }

  @Override
  @NotNull
  public CompletableFuture<List<T>> findAll(final @NotNull QueryFilter<T> filter) {
    return CompletableFuture.supplyAsync(() -> {
      checkConnection();
      
      List<T> result = entities.stream()
          .filter(buildPredicate(filter))
          .toList();
      
      // Apply sorting
      if (filter.sort() != null) {
        result = new ArrayList<>(result);
        result.sort((a, b) -> {
          final Object valA = getFieldValue(a, filter.sort().field());
          final Object valB = getFieldValue(b, filter.sort().field());
          
          @SuppressWarnings("unchecked")
          final int cmp = ((Comparable<Object>) valA).compareTo(valB);
          
          return filter.sort().direction() == QueryFilter.Direction.ASCENDING ? cmp : -cmp;
        });
      }
      
      // Apply limit
      if (filter.limit() != null && result.size() > filter.limit()) {
        result = result.subList(0, filter.limit());
      }
      
      return result;
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Optional<T>> find(final @NotNull QueryFilter<T> filter) {
    return CompletableFuture.supplyAsync(() -> {
      checkConnection();
      
      return entities.stream()
          .filter(buildPredicate(filter))
          .findFirst();
    });
  }

  @Override
  @NotNull
  public CompletableFuture<List<T>> findBatch(final @NotNull Collection<? extends QueryFilter<T>> filters) {
    return CompletableFuture.supplyAsync(() -> {
      checkConnection();
      
      final List<T> results = new ArrayList<>();
      
      for (final QueryFilter<T> filter : filters) {
        entities.stream()
            .filter(buildPredicate(filter))
            .forEach(results::add);
      }
      
      return results;
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> delete(final @NotNull QueryFilter<T> filter) {
    return CompletableFuture.runAsync(() -> {
      checkConnection();
      
      final Predicate<T> predicate = buildPredicate(filter);
      final List<T> toRemove = entities.stream()
          .filter(predicate)
          .toList();
      
      for (final T entity : toRemove) {
        final Object id = extractId(entity);
        entities.remove(entity);
        if (id != null) {
          entityIndex.remove(id);
        }
      }
    });
  }

  // ==================== Test Helper Methods ====================

  /**
   * Returns all stored entities (for test verification).
   * 
   * @return list of all entities
   */
  @NotNull
  public List<T> getAllEntities() {
    return new ArrayList<>(entities);
  }

  /**
   * Returns an entity by its ID (for test verification).
   * 
   * @param id the entity ID
   * @return the entity or null
   */
  public T getById(final @NotNull Object id) {
    return entityIndex.get(id);
  }

  /**
   * Returns the number of stored entities (for test verification).
   * 
   * @return the count
   */
  public int size() {
    return entities.size();
  }

  /**
   * Checks if an entity with the given ID exists (for test verification).
   * 
   * @param id the entity ID
   * @return true if exists
   */
  public boolean containsId(final @NotNull Object id) {
    return entityIndex.containsKey(id);
  }

  /**
   * Clears all stored entities (for test cleanup).
   */
  public void clear() {
    entities.clear();
    entityIndex.clear();
  }

  /**
   * Checks if the provider is connected.
   * 
   * @return true if connected
   */
  public boolean isConnected() {
    return connected;
  }

  /**
   * Simulates disconnection (for testing error scenarios).
   */
  public void disconnect() {
    connected = false;
  }

  /**
   * Adds an entity directly (bypasses write logic, for test setup).
   * 
   * @param entity the entity to add
   */
  public void addDirect(final @NotNull T entity) {
    entities.add(entity);
    final Object id = extractId(entity);
    if (id != null) {
      entityIndex.put(id, entity);
    }
  }

  // ==================== Internal Methods ====================

  private void checkConnection() {
    if (!connected) {
      throw new IllegalStateException("InMemoryDatabaseProvider is not connected");
    }
  }

  private Object extractId(final T entity) {
    if (entity == null) {
      return null;
    }
    
    final Class<?> clazz = entity.getClass();
    
    // Handle records
    if (clazz.isRecord()) {
      for (final RecordComponent component : clazz.getRecordComponents()) {
        if (component.isAnnotationPresent(Identifier.class)) {
          try {
            return component.getAccessor().invoke(entity);
          } catch (final Exception e) {
            throw new RuntimeException("Failed to extract ID from record", e);
          }
        }
      }
      
      // Default: use first component named "id" or "_id"
      for (final RecordComponent component : clazz.getRecordComponents()) {
        final String name = component.getName();
        if ("id".equals(name) || "_id".equals(name)) {
          try {
            return component.getAccessor().invoke(entity);
          } catch (final Exception e) {
            throw new RuntimeException("Failed to extract ID from record", e);
          }
        }
      }
    }
    
    // Handle regular classes
    for (final var field : clazz.getDeclaredFields()) {
      if (field.isAnnotationPresent(Identifier.class)) {
        try {
          field.setAccessible(true);
          return field.get(entity);
        } catch (final Exception e) {
          throw new RuntimeException("Failed to extract ID from field", e);
        }
      }
    }
    
    return null;
  }

  private Object getFieldValue(final T entity, final String fieldName) {
    if (entity == null) {
      return null;
    }
    
    final Class<?> clazz = entity.getClass();
    
    // Handle records
    if (clazz.isRecord()) {
      for (final RecordComponent component : clazz.getRecordComponents()) {
        final String name = getEffectiveFieldName(component);
        if (name.equals(fieldName)) {
          try {
            return component.getAccessor().invoke(entity);
          } catch (final Exception e) {
            throw new RuntimeException("Failed to get field value from record", e);
          }
        }
      }
    }
    
    // Handle regular classes
    for (final var field : clazz.getDeclaredFields()) {
      final String name = getEffectiveFieldName(field);
      if (name.equals(fieldName)) {
        try {
          field.setAccessible(true);
          return field.get(entity);
        } catch (final Exception e) {
          throw new RuntimeException("Failed to get field value", e);
        }
      }
    }
    
    // Try direct field access as fallback
    try {
      final var field = clazz.getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(entity);
    } catch (final NoSuchFieldException e) {
      return null;
    } catch (final Exception e) {
      throw new RuntimeException("Failed to get field value", e);
    }
  }

  private String getEffectiveFieldName(final RecordComponent component) {
    if (component.isAnnotationPresent(Property.class)) {
      final String value = component.getAnnotation(Property.class).value();
      return value.isEmpty() ? component.getName() : value;
    }
    if (component.isAnnotationPresent(Identifier.class)) {
      return "_id";
    }
    return component.getName();
  }

  private String getEffectiveFieldName(final java.lang.reflect.Field field) {
    if (field.isAnnotationPresent(Property.class)) {
      final String value = field.getAnnotation(Property.class).value();
      return value.isEmpty() ? field.getName() : value;
    }
    if (field.isAnnotationPresent(Identifier.class)) {
      return "_id";
    }
    return field.getName();
  }

  @SuppressWarnings("unchecked")
  private Predicate<T> buildPredicate(final QueryFilter<T> filter) {
    if (filter.conditions().isEmpty()) {
      return _ -> true;
    }
    
    return entity -> {
      for (final QueryFilter.Condition condition : filter.conditions()) {
        final Object fieldValue = getFieldValue(entity, condition.field());
        final Object filterValue = condition.value();
        
        final boolean matches = switch (condition.operator()) {
          case EQ -> {
            if (fieldValue == null && filterValue == null) {
              yield true;
            }
            if (fieldValue == null || filterValue == null) {
              yield false;
            }
            yield fieldValue.equals(filterValue);
          }
          case NE -> !java.util.Objects.equals(fieldValue, filterValue);
          case GT -> {
            if (fieldValue == null || filterValue == null) {
              yield false;
            }
            yield ((Comparable<Object>) fieldValue).compareTo(filterValue) > 0;
          }
          case GTE -> {
            if (fieldValue == null || filterValue == null) {
              yield false;
            }
            yield ((Comparable<Object>) fieldValue).compareTo(filterValue) >= 0;
          }
          case LT -> {
            if (fieldValue == null || filterValue == null) {
              yield false;
            }
            yield ((Comparable<Object>) fieldValue).compareTo(filterValue) < 0;
          }
          case LTE -> {
            if (fieldValue == null || filterValue == null) {
              yield false;
            }
            yield ((Comparable<Object>) fieldValue).compareTo(filterValue) <= 0;
          }
          case IN -> {
            if (filterValue instanceof Collection<?> col) {
              yield col.contains(fieldValue);
            }
            yield false;
          }
          case CONTAINS -> {
            if (fieldValue instanceof String str && filterValue instanceof String pattern) {
              yield str.contains(pattern);
            }
            yield false;
          }
          case STARTS_WITH -> {
            if (fieldValue instanceof String str && filterValue instanceof String pattern) {
              yield str.startsWith(pattern);
            }
            yield false;
          }
          case ENDS_WITH -> {
            if (fieldValue instanceof String str && filterValue instanceof String pattern) {
              yield str.endsWith(pattern);
            }
            yield false;
          }
          case EXISTS -> fieldValue != null;
          case NOT_EXISTS -> fieldValue == null;
        };
        
        if (!matches) {
          return false;
        }
      }
      
      return true;
    };
  }
}
