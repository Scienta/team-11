package no.scienta.cfp;

import java.text.DateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Times as the legacy clients show them.
 */
public final class DisplayFormats {

    /**
     * An explicit pattern, not the locale's SHORT style: from Java 20 the SHORT style puts a narrow no-break space
     * before AM/PM, and the reviewer UI needs an ordinary space.
     */
    private static final DateTimeFormatter US_SHORT =
            DateTimeFormatter.ofPattern("M/d/yy, h:mm a", Locale.US).withZone(ZoneOffset.UTC);

    private DisplayFormats() {
    }

    /** "11/4/26, 1:05 PM" in UTC, what the reviewer UI shows. */
    public static String usShort(Instant instant) {
        return US_SHORT.format(instant);
    }

    /** "4. nov. 2026" in Oslo, for the committee spreadsheet. */
    public static String norwegianDate(Date date) {
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM, new Locale("nb", "NO"));
        format.setTimeZone(TimeZone.getTimeZone("Europe/Oslo"));
        return format.format(date);
    }
}
