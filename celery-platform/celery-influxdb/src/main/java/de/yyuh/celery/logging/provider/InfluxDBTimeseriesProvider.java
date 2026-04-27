package de.yyuh.celery.logging.provider;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.entity.ILogEntry;
import de.yyuh.celery.api.provider.ITimeseriesProvider;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.libs.core.result.Result;
import de.yyuh.libs.core.timer.Timer;

public final class InfluxDBTimeseriesProvider implements ITimeseriesProvider {

  private InfluxDBClient influxDBClient;
  private String organization;
  private String bucket;

  @Override
  public @NotNull CompletableFuture<Result<Long, String>> connect(final @NotNull Credentials credentials) {
    final var timer = Timer.start();

    return CompletableFuture.supplyAsync(() -> Result.of(() -> {
      final String url = String.format("http://%s:%d", credentials.ip(), credentials.port());
      this.organization = "celery";
      this.bucket = "logging";

      this.influxDBClient = InfluxDBClientFactory.create(url, credentials.password().toCharArray(), organization,
          bucket);
      return timer.end();
    }).mapErr(Exception::getMessage));
  }

  @Override
  public @NotNull CompletableFuture<Void> delete(final @NotNull Instant timestamp) {
    return CompletableFuture.runAsync(() -> {
      if (influxDBClient != null) {
        final var offSet = OffsetDateTime.ofInstant(timestamp, ZoneId.systemDefault());

        influxDBClient.getDeleteApi().delete(
            offSet.minusSeconds(1),
            offSet.plusSeconds(1),
            "",
            bucket,
            organization);
      }
    });
  }

  @Override
  public @NotNull CompletableFuture<List<ILogEntry>> find(final @NotNull IQuery<ILogEntry> query) {
    return CompletableFuture.supplyAsync(() -> {
      return List.of();
    });
  }

  @Override
  public @NotNull CompletableFuture<Void> save(final @NotNull ILogEntry entity) {
    return CompletableFuture.runAsync(() -> {
      if (influxDBClient == null) {
        throw new IllegalStateException("Not connected to InfluxDB");
      }

      final WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

      final Point point = Point.measurement("log_entry")
          .addTag("level", "INFO")
          .addField("message", entity.toString())
          .time(Instant.now(), WritePrecision.MS);

      writeApi.writePoint(bucket, organization, point);
    });
  }

  @Override
  public void close() {
    if (this.influxDBClient != null) {
      this.influxDBClient.close();
      this.influxDBClient = null;
    }
  }

  @Override
  public @NotNull CompletableFuture<Boolean> isConnected() {
    return CompletableFuture.supplyAsync(() -> {
      if (this.influxDBClient == null) {
        return false;
      }

      try {
        this.influxDBClient.ping();
        return true;
      } catch (Exception e) {
        return false;
      }
    });
  }
}
