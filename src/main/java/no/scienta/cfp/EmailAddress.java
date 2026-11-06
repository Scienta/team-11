package no.scienta.cfp;

import java.util.regex.Pattern;

/**
 * What counts as a real email address (Speakers R3).
 */
public final class EmailAddress {

    /** The RFC 5321 limits, in characters. The log masking in {@code RequestLogFilter} relies on them. */
    private static final int LONGEST_LOCAL_PART = 64;
    private static final int LONGEST_DOMAIN = 255;

    /**
     * One {@code @}, a local part, and a domain of at least two dot-separated labels. No whitespace, Unicode
     * whitespace such as a no-break space included.
     */
    private static final Pattern EMAIL = Pattern.compile(
            "[^@\\s]{1," + LONGEST_LOCAL_PART + "}@(?=[^@\\s]{1," + LONGEST_DOMAIN + "}$)[^@\\s.]+(\\.[^@\\s.]+)+",
            Pattern.UNICODE_CHARACTER_CLASS);

    private EmailAddress() {
    }

    public static boolean isValid(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }
}
