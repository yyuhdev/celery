package de.yyuh.libs.core.timer;

import org.jetbrains.annotations.NotNull;
import java.util.concurrent.TimeUnit;

public final class Timer {

    private final long startTime;

    private Timer(final long startTime) {
        this.startTime = startTime;
    }

    @NotNull
    public static Timer start() {
        return new Timer(System.currentTimeMillis());
    }

    public long end() {
        return System.currentTimeMillis() - this.startTime;
    }

    public double toSeconds() {
        return this.end() / 1000.0;
    }

    public long toTimeUnit(final @NotNull  TimeUnit timeUnit) {
        return timeUnit.convert(this.end(), TimeUnit.MILLISECONDS);
    }
}
