package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.junit.TypeConverter;

/**
 * The time format of ratedAtDisplay for the legacy reviewer UI (ratings R8). Calls the formatter directly, because the
 * service takes the time of a rating from the system clock.
 */
@DisplayName("Ratings: rated-at display")
public class RatedAtDisplayTest {

    @DisplayName("R8: Shows the rating time in UTC as M/d/yy, h:mm AM")
    @Description("""
            The expected values have exactly one ordinary space before AM or PM, as the reviewer UI
            needs. The service takes the time of the latest rating from its clock, which a test cannot
            set, so this table checks only the format, through the formatter that PUT
            /talks/{id}/ratings/{reviewerId} uses for ratedAtDisplay.
            """)
    @TableTest("""
        Scenario                        | Rated At                  | Shown?
        Afternoon                       | 2026-11-04T13:05:00Z      | 11/4/26, 1:05 PM
        Midnight                        | 2026-11-04T00:00:00Z      | 11/4/26, 12:00 AM
        Noon                            | 2026-11-04T12:00:00Z      | 11/4/26, 12:00 PM
        Two-digit month, day and hour   | 2026-12-24T10:30:00Z      | 12/24/26, 10:30 AM
        Seconds in the time             | 2026-11-04T13:05:59Z      | 11/4/26, 1:05 PM
        Time given in another time zone | 2026-11-04T23:30:00-02:00 | 11/5/26, 1:30 AM
        """)
    void showsTheRatingTimeInUtc(Date ratedAt, String shown) {
        assertEquals(shown, Utils.usShort(ratedAt));
    }

    @TypeConverter
    public static Date parseInstant(String isoDateTime) {
        return Date.from(OffsetDateTime.parse(isoDateTime).toInstant());
    }
}
