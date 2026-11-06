package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.CsvSource;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.junit.TypeConverter;

/**
 * The small helpers in {@link Utils}: text checks, number and date formats, and values for the CSV export.
 */
public class UtilsTest {

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
            "ada\u00a0lovelace@example.com,       false",
            "(64 × a)@example.com,                true",
            "(65 × a)@example.com,                false",
            "ada@(251 × e).com,                   true",
            "ada@(252 × e).com,                   false",
    })
    void acceptsOnlyARealEmailAddress(@ConvertWith(RepeatedCharacters.class) String email, boolean valid) {
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

    @DisplayName("Exports a value that starts like a formula as text")
    @Description("""
            A spreadsheet runs a cell that starts with =, +, - or @ as a formula, also after a tab. The
            export of /admin/talks.csv puts ' in front, as the OWASP advice on CSV injection says. A
            title such as "-1 tips" also gets the ', because the export cannot tell it from a formula.
            In a cell, \\t is a tab.
            """)
    @TableTest("""
        Scenario                     | Value           | Exported?
        Starts with =                | =1+1            | "'=1+1"
        Starts with +                | +47 22 33 44 55 | "'+47 22 33 44 55"
        Starts with -                | -1 tips         | "'-1 tips"
        Starts with @                | @SUM(A1)        | "'@SUM(A1)"
        Starts with a tab            | \\t=1+1         | "'\\t=1+1"
        Formula sign after the start | 1+1=2           | 1+1=2
        """)
    void exportsAValueThatStartsLikeAFormulaAsText(CsvText value, CsvText exported) {
        assertEquals(exported.value(), Utils.csv(value.value()));
    }

    @DisplayName("Quotes a value with a semicolon or a line break")
    @Description("""
            The export separates the columns with ; and the talks with a line break. In a cell, \\n is a
            line feed and \\r is a carriage return.
            """)
    @TableTest("""
        Scenario        | Value                    | Exported?
        Plain text      | Legacy is a feeling      | Legacy is a feeling
        Semicolon       | Legacy; a feeling        | '"Legacy; a feeling"'
        Line feed       | Legacy\\nis a feeling    | '"Legacy\\nis a feeling"'
        Carriage return | Legacy\\r\\nis a feeling | '"Legacy\\r\\nis a feeling"'
        """)
    void quotesAValueWithASemicolonOrALineBreak(CsvText value, CsvText exported) {
        assertEquals(exported.value(), Utils.csv(value.value()));
    }

    @Test
    void putsTheFormulaMarkInsideTheCsvQuotes() {
        assertEquals("\"'=1;2\"", Utils.csv("=1;2"));
        assertEquals("\"'\r=1+1\"", Utils.csv("\r=1+1"));
    }

    /** A CSV value as a table cell writes it: \\t, \\r and \\n stand for a tab, a carriage return and a line feed. */
    @TypeConverter
    public static CsvText parseCsvText(String cell) {
        return new CsvText(cell.replace("\\t", "\t").replace("\\r", "\r").replace("\\n", "\n"));
    }

    record CsvText(String value) {
    }

    /** Expands a run written as {@code (64 × a)} into the letter a written 64 times. */
    static class RepeatedCharacters extends SimpleArgumentConverter {
        private static final Pattern RUN = Pattern.compile("\\((\\d+) × (.)\\)");

        @Override
        protected Object convert(Object source, Class<?> targetType) {
            return RUN.matcher((String) source).replaceAll(run -> run.group(2).repeat(Integer.parseInt(run.group(1))));
        }
    }
}
