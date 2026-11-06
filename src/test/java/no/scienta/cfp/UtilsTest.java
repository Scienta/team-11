package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UtilsTest {

    @Test
    void isEmpty() {
        assertTrue(Utils.isEmpty(null));
        assertTrue(Utils.isEmpty(""));
        assertTrue(Utils.isEmpty("   "));
        assertFalse(Utils.isEmpty("a"));
    }

    @Test
    void isNumeric() {
        assertTrue(Utils.isNumeric("123"));
        assertFalse(Utils.isNumeric("12a"));
        assertFalse(Utils.isNumeric(""));
    }

    @Test
    void capitalize() {
        assertEquals("Workshop", Utils.capitalize("WORKSHOP"));
    }

    // Averages are shown with one decimal, cut off (CFP-61)
    @Test
    void roundsRatingToOneDecimal() {
        assertEquals(4.2, Utils.roundRating(4.25));
        assertEquals(3.6, Utils.roundRating(3.6666));
        assertEquals(4.5, Utils.roundRating(4.5));
    }

    @Disabled("flaky on Jenkins")
    @Test
    void nowIsCurrentTime() {
        SimpleDateFormat f = new SimpleDateFormat(Utils.DB_DATE_TIME);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals(f.format(new Date()), Utils.now());
    }

    // Speakers R3; the rows are the example table in workflow/specs/speakers/spec.md
    @ParameterizedTest(name = "{0} is an email: {1}")
    @CsvSource({
            "ada@example.com,                     true",
            "ada.lovelace+cfp@mail.example.co.uk, true",
            "not-an-email,                        false",
            "ada@example,                         false",
            "@example.com,                        false",
            "ada@.com,                            false",
            "ada@example.,                        false",
            "ada@example..com,                    false",
            "ada@@example.com,                    false",
            "ada lovelace@example.com,            false",
            "' ada@example.com',                  false",
    })
    void acceptsOnlyARealEmailAddress(String email, boolean valid) {
        assertEquals(valid, Utils.isValidEmail(email));
    }

    // Ratings R8: the same on every JDK; Java 20+ puts a narrow no-break space before AM in the locale's short style
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "2026-11-04 13:05:00, '11/4/26, 1:05 PM'",
            "2026-11-04 00:00:00, '11/4/26, 12:00 AM'",
            "2026-12-31 12:00:59, '12/31/26, 12:00 PM'",
            "2026-01-09 09:30:00, '1/9/26, 9:30 AM'",
    })
    void showsTheRatingTimeInTheUsShortFormat(String utcTime, String shown) {
        assertEquals(shown, Utils.usShort(Utils.parse(utcTime)));
    }
}
