package club.revived.celery.database.model;

import java.time.Instant;

import org.jetbrains.annotations.NotNull;

import com.influxdb.annotations.Column;

import club.revived.celery.database.Compositio;

public abstract class LogMetric {

  private final @NotNull Compositio compositio = Compositio.instance();

  @Column(timestamp = true)
  protected @NotNull Instant timestamp = Instant.now();

  protected LogMetric() {
  }

  @SuppressWarnings("unchecked")
  public void write() {
    final var clazz = (Class<LogMetric>) this.getClass();
    compositio.write(clazz, this);
  }
}
