package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class TimestampsTest {

    private final Timestamps timestamps =
            new Timestamps(Clock.fixed(Instant.parse("2026-11-06T23:34:56.789Z"), ZoneId.of("Europe/Oslo")));

    @Test
    void nowIsTheClocksTime() {
        assertEquals(Instant.parse("2026-11-06T23:34:56.789Z"), timestamps.now());
    }

    @Test
    void writesTheDatabaseTimeInUtcToTheSecond() {
        assertEquals("2026-11-06 23:34:56", Timestamps.databaseTime(timestamps.now()));
    }

    @Test
    void writesTodayInTheClocksTimeZone() {
        assertEquals("07.11.2026", timestamps.today());
    }
}
