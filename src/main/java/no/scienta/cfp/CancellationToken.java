package no.scienta.cfp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * The secret that lets the attendee who registered cancel the registration (workshop-registration R10): 32 random bytes
 * in URL-safe Base64. The database keeps only its SHA-256 hash, and {@link #toString()} never shows it.
 */
public record CancellationToken(String value) {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RANDOM_BYTES = 32;

    /** A new token that nobody can guess. */
    public static CancellationToken random() {
        byte[] bytes = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(bytes);
        return new CancellationToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    /** The token in the body of an unregistration, or empty when {@code token} is missing or not a JSON string. */
    public static Optional<CancellationToken> parse(Map<String, Object> body) {
        return body.get("token") instanceof String token ? Optional.of(new CancellationToken(token)) : Optional.empty();
    }

    /** The SHA-256 hash of the token in lower-case hex, as the database keeps it. */
    public String hash() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("every Java runtime has SHA-256", e);
        }
    }

    /** Whether this token has the stored hash. Takes the same time wherever the hashes differ. */
    public boolean matches(String storedHash) {
        return MessageDigest.isEqual(hash().getBytes(StandardCharsets.US_ASCII),
                storedHash.getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public String toString() {
        return "CancellationToken[***]";
    }
}
