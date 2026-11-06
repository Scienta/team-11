package no.scienta.cfp;

import java.util.Optional;

/**
 * What a talk list shows (Ratings R5, R6, Acceptance R9): its order, the talks of one speaker or of all speakers, and
 * whether the withdrawn talks are in it. Read from the parameters of {@code GET /talks} by {@link #parse}.
 */
public record TalkListQuery(TalkOrder order, Optional<Long> speaker, boolean includeWithdrawn) {

    /** Every talk, oldest first, the withdrawn talks last: the list of the reports. */
    public static final TalkListQuery EVERY_TALK = new TalkListQuery(TalkOrder.OLDEST_FIRST, Optional.empty(), true);

    /** The query, or why a parameter is not a value of it. */
    public sealed interface Parsed {
    }

    /** The parameters make a query. */
    public record Valid(TalkListQuery query) implements Parsed {
    }

    /** A parameter is not a value of the query; the reason is the message of the 400 answer. */
    public record Invalid(String reason) implements Parsed {
    }

    /**
     * The query from the parameters of {@code GET /talks}. {@code includeWithdrawn} is checked first, then {@code sort}.
     * Only {@code true} or {@code false} include or leave out the withdrawn talks, not the other spellings that Spring
     * takes for a boolean.
     */
    public static Parsed parse(String sort, Long speakerId, String includeWithdrawn) {
        if (!includeWithdrawn.equals("true") && !includeWithdrawn.equals("false")) {
            return new Invalid("includeWithdrawn must be true or false");
        }
        return TalkOrder.parse(sort)
                .<Parsed>map(order -> new Valid(
                        new TalkListQuery(order, Optional.ofNullable(speakerId), Boolean.parseBoolean(includeWithdrawn))))
                .orElseGet(() -> new Invalid("sort must be 'rating'"));
    }
}
