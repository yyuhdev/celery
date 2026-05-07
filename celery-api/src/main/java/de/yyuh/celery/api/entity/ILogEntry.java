package de.yyuh.celery.api.entity;

/**
 * Marker interface for time-series log entries.
 *
 * <p>Implementations of this interface represent log data that can be
 * persisted to a time-series database via {@link de.yyuh.celery.api.provider.ITimeseriesProvider}.
 *
 * @see de.yyuh.celery.api.provider.ITimeseriesProvider
 */
public interface ILogEntry {

}
