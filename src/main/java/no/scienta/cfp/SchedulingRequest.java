package no.scienta.cfp;

import java.util.Map;
import java.util.Optional;

/**
 * The committee's request to put a talk on the programme, from the body of {@code PUT /programme/{room}/{slot}}
 * (scheduling R2). Only {@link #parse} creates one, so the talk ID is always a JSON integer.
 */
public record SchedulingRequest(long talkId) {

    /** The request in the body, or empty when {@code talkId} is missing, null, or not a JSON integer. */
    public static Optional<SchedulingRequest> parse(Map<String, Object> body) {
        Object talkId = body.get("talkId");
        if (talkId instanceof Integer || talkId instanceof Long) {
            return Optional.of(new SchedulingRequest(((Number) talkId).longValue()));
        }
        return Optional.empty();
    }
}
