package no.scienta.cfp;

import java.util.Optional;

/**
 * The order of a talk list (Ratings R5, R6): oldest first, or ranked by rating. Read from the {@code sort} parameter of
 * {@code GET /talks}.
 */
public enum TalkOrder {
    /** By submission, oldest first: no {@code sort} parameter (Ratings R5). */
    OLDEST_FIRST,
    /** Ranked by rating, the unrated talks last: {@code sort=rating} (Ratings R6). */
    BY_RATING;

    /** The order that the {@code sort} parameter asks for, or empty when the value is not an order. */
    public static Optional<TalkOrder> parse(String sort) {
        if (sort == null) {
            return Optional.of(OLDEST_FIRST);
        }
        return "rating".equals(sort) ? Optional.of(BY_RATING) : Optional.empty();
    }
}
