package no.scienta.cfp;

/**
 * Values made fit for one field of the semicolon CSV export.
 */
public final class CsvField {

    private CsvField() {
    }

    /**
     * Safe for a spreadsheet: shown as text, never as a formula, and kept in one cell. A value that starts like a
     * formula gets a leading {@code '}. Quoted when it holds a {@code ;}, a {@code "} or a line break, with each
     * {@code "} replaced by {@code '}. Null is empty.
     */
    public static String text(String value) {
        if (value == null) {
            return "";
        }
        String text = startsLikeAFormula(value) ? "'" + value : value;
        if (text.contains(";") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "'") + "\"";
        }
        return text;
    }

    private static boolean startsLikeAFormula(String value) {
        return !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0;
    }

    /** "WORKSHOP" as "Workshop". Blank stays as it is. */
    public static String capitalized(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }
}
