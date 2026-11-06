package no.scienta.cfp;

/**
 * Values made fit for one field of the semicolon CSV export.
 */
public final class CsvField {

    private CsvField() {
    }

    /** Quoted when it holds a {@code ;} or a {@code "}, with each {@code "} replaced by {@code '}. Null is empty. */
    public static String text(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(";") || value.contains("\"")) {
            return "\"" + value.replace("\"", "'") + "\"";
        }
        return value;
    }

    /** "WORKSHOP" as "Workshop". Blank stays as it is. */
    public static String capitalized(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }
}
