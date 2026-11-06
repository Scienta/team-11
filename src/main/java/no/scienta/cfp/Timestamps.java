package no.scienta.cfp;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

/**
 * The current time from the clock, and the text forms the database keeps it in.
 */
@Component
public class Timestamps {

    private static final DateTimeFormatter DATABASE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter LEGACY_DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final Clock clock;

    public Timestamps(Clock clock) {
        this.clock = clock;
    }

    public Instant now() {
        return clock.instant();
    }

    /** Today in the clock's time zone, as the old admin UI wrote it: {@code dd.MM.yyyy}. */
    public String today() {
        return LEGACY_DAY.format(LocalDate.now(clock));
    }

    /** UTC, to the second: {@code yyyy-MM-dd HH:mm:ss}. */
    public static String databaseTime(Instant instant) {
        return DATABASE_TIME.format(instant);
    }
}
