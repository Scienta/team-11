package no.scienta.cfp;

import java.util.Map;
import java.util.Optional;

/**
 * The committee's decision on a talk, from the body of {@code PUT /talks/{id}/status}: one of the three decisions
 * of Acceptance R1.
 */
public record Decision(TalkStatus status) {

    /** The decision in the body, or empty when the body holds no decision. */
    public static Optional<Decision> parse(Map<String, Object> body) {
        return TalkStatus.parseDecision(body.get("status")).map(Decision::new);
    }
}
