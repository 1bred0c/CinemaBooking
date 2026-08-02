package congtuong.dev.cinemabooking.ai.tool;

import java.time.LocalTime;
import java.util.Locale;

public enum ShowtimePeriod {
    ANY(LocalTime.MIN, LocalTime.MAX),
    MORNING(LocalTime.of(5, 0), LocalTime.NOON),
    AFTERNOON(LocalTime.NOON, LocalTime.of(18, 0)),
    EVENING(LocalTime.of(18, 0), LocalTime.of(23, 0)),
    NIGHT(LocalTime.of(23, 0), LocalTime.MAX);

    private final LocalTime start;
    private final LocalTime end;

    ShowtimePeriod(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    public LocalTime start() {
        return start;
    }

    public LocalTime end() {
        return end;
    }

    public static ShowtimePeriod fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return ANY;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return ANY;
        }
    }
}
