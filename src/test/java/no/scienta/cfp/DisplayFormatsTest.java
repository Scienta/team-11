package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DisplayFormatsTest {

    // Ratings R8: the same on every JDK; Java 20+ puts a narrow no-break space before AM in the locale's short style
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "2026-11-04T13:05:00Z, '11/4/26, 1:05 PM'",
            "2026-11-04T00:00:00Z, '11/4/26, 12:00 AM'",
            "2026-12-31T12:00:59Z, '12/31/26, 12:00 PM'",
            "2026-01-09T09:30:00Z, '1/9/26, 9:30 AM'",
    })
    void showsTheRatingTimeInTheUsShortFormat(String utcTime, String shown) {
        assertEquals(shown, DisplayFormats.usShort(Instant.parse(utcTime)));
    }

    @Test
    void showsTheSubmissionDayInOslo() {
        assertEquals("7. nov. 2026", DisplayFormats.norwegianDate(Date.from(Instant.parse("2026-11-06T23:30:00Z"))));
    }
}
