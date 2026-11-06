package no.scienta.cfp;

import java.util.Map;
import java.util.Optional;

/**
 * A speaker's request to withdraw a talk, from the body of {@code POST /talks/{id}/withdraw} (Acceptance R4).
 */
public record Withdrawal(long speakerId) {

    /** The withdrawal in the body, or empty when {@code speakerId} is missing or not a JSON integer. */
    public static Optional<Withdrawal> parse(Map<String, Object> body) {
        Object speakerId = body.get("speakerId");
        return speakerId instanceof Integer || speakerId instanceof Long
                ? Optional.of(new Withdrawal(((Number) speakerId).longValue()))
                : Optional.empty();
    }
}
