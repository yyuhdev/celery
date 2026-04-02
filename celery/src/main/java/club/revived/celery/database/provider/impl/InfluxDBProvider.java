package club.revived.celery.database.provider.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.WritePrecision;

import club.revived.celery.database.model.DatabaseCredentials;
import club.revived.celery.database.model.LogMetric;
import club.revived.celery.database.model.query.QueryFilter;
import club.revived.celery.database.provider.DatabaseProvider;

public final class InfluxDBProvider implements DatabaseProvider<LogMetric> {

  @Nullable
  private InfluxDBClient client;

  @Nullable
  private QueryApi queryApi;

  @Nullable
  private String bucket;

  @Nullable
  private String org;

  @Override
  public void connect(final @NotNull DatabaseCredentials credentials) {
    final var url = credentials.host();
    final var token = credentials.password().toCharArray();
    org = credentials.user();
    bucket = credentials.database();

    client = InfluxDBClientFactory.create(url, token, org, bucket);
    queryApi = client.getQueryApi();

    if (client.getBucketsApi().findBucketByName(bucket) == null) {
      throw new IllegalStateException("The bucket " + bucket + " does not exist!");
    }
  }

  @Override
  @NotNull
  public CompletableFuture<Void> write(final @NotNull LogMetric metric) {
    return CompletableFuture.runAsync(() -> {
      requireConnection();
      final var writeApi = client.getWriteApiBlocking();
      writeApi.writeMeasurement(bucket, org, WritePrecision.NS, metric);
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> writeBatch(final @NotNull List<LogMetric> metrics) {
    return CompletableFuture.runAsync(() -> {
      if (metrics.isEmpty()) {
        return;
      }

      requireConnection();
      final var writeApi = client.getWriteApiBlocking();
      writeApi.writeMeasurements(bucket, org, WritePrecision.NS, metrics);
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Optional<LogMetric>> find(final @NotNull QueryFilter<LogMetric> filter) {
    return CompletableFuture.supplyAsync(() -> {
      final var results = querySingle(filter);
      return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    });
  }

  @Override
  @NotNull
  public CompletableFuture<List<LogMetric>> findBatch(
      final @NotNull Collection<? extends QueryFilter<LogMetric>> filters) {
    return CompletableFuture.supplyAsync(() -> filters.stream()
        .flatMap(f -> querySingle(f).stream())
        .collect(Collectors.toList()));
  }

  @NotNull
  private List<LogMetric> querySingle(final @NotNull QueryFilter<LogMetric> filter) {
    requireConnection();
    final var fluxQuery = buildFluxQuery(filter);
    return queryApi.query(fluxQuery, LogMetric.class);
  }

  @Override
  @NotNull
  public CompletableFuture<List<LogMetric>> findAll() {
    return CompletableFuture.supplyAsync(() -> {
      requireConnection();
      return queryApi.query("from(bucket: \"" + bucket + "\") |> range(start: 0)", LogMetric.class);
    });
  }

  @Override
  @NotNull
  public CompletableFuture<List<LogMetric>> findAll(final @NotNull QueryFilter<LogMetric> filter) {
    return CompletableFuture.supplyAsync(() -> querySingle(filter));
  }

  @Override
  @NotNull
  public CompletableFuture<Void> delete(final @NotNull QueryFilter<LogMetric> filter) {
    return CompletableFuture.runAsync(() -> {
      requireConnection();
      final var deleteApi = client.getDeleteApi();

      final OffsetDateTime start = filter.time().from().atOffset(ZoneOffset.UTC);
      final OffsetDateTime stop = filter.time().to().atOffset(ZoneOffset.UTC);
      final var predicate = buildDeletePredicate(filter);

      deleteApi.delete(start, stop, predicate, bucket, org);
    });
  }

  @NotNull
  private String buildFluxQuery(final @NotNull QueryFilter<LogMetric> filter) {
    final var sb = new StringBuilder();

    sb.append("from(bucket: \"").append(bucket).append("\")\n");
    sb.append("  |> range(start: ").append(filter.time() != null ? filter.time().from() : "-1h").append(", ");
    sb.append("stop: ").append(filter.time() != null ? filter.time().to() : "now()").append(")\n");

    for (final var cond : filter.conditions()) {
      sb.append("  |> filter(fn: (r) => ");
      switch (cond.operator()) {
        case CONTAINS -> sb.append("r[\"").append(cond.field()).append("\"] =~ /.*").append(cond.value()).append(".*/");
        case STARTS_WITH -> sb.append("r[\"").append(cond.field()).append("\"] =~ /^").append(cond.value()).append("/");
        case ENDS_WITH -> sb.append("r[\"").append(cond.field()).append("\"] =~ /").append(cond.value()).append("$/");
        case IN -> throw new UnsupportedOperationException("IN operator is not supported in Flux queries.");
        case EXISTS, NOT_EXISTS ->
          throw new UnsupportedOperationException("EXISTS/NOT_EXISTS is not supported in InfluxDB.");
        default -> {
          sb.append("r[\"").append(cond.field()).append("\"] ");
          sb.append(operatorToFlux(cond.operator())).append(" ");
          sb.append(formatValue(cond.value()));
        }
      }
      sb.append(")\n");
    }

    if (filter.sort() != null) {
      sb.append("  |> sort(columns: [\"").append(filter.sort().field()).append("\"], desc: ");
      sb.append(filter.sort().direction() == QueryFilter.Direction.DESCENDING).append(")\n");
    }

    if (filter.limit() != null) {
      sb.append("  |> limit(n: ").append(filter.limit()).append(")\n");
    }

    return sb.toString();
  }

  @NotNull
  private String buildDeletePredicate(final @NotNull QueryFilter<LogMetric> filter) {
    if (filter.conditions().isEmpty()) {
      return "";
    }

    return filter.conditions().stream()
        .map(cond -> {
          if (cond.operator() == QueryFilter.Operator.IN) {
            throw new UnsupportedOperationException("IN operator not supported for InfluxDB delete.");
          }
          return cond.field() + " " + operatorToFlux(cond.operator()) + " " + formatValue(cond.value());
        })
        .collect(Collectors.joining(" AND "));
  }

  @NotNull
  private String operatorToFlux(final @NotNull QueryFilter.Operator op) {
    return switch (op) {
      case EQ -> "==";
      case NE -> "!=";
      case GT -> ">";
      case GTE -> ">=";
      case LT -> "<";
      case LTE -> "<=";
      default -> throw new UnsupportedOperationException(op.name() + " operator is not supported directly.");
    };
  }

  @NotNull
  private String formatValue(final @NotNull Object value) {
    if (value instanceof String) {
      return "\"" + value + "\"";
    }
    return value.toString();
  }

  private void requireConnection() {
    if (client == null || queryApi == null) {
      throw new IllegalStateException("InfluxDB is not connected");
    }
  }
}
