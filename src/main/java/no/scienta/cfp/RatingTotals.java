package no.scienta.cfp;

/**
 * The ratings of one talk as the talk lists show them: how many reviewers rated it, and the sum of their scores.
 */
public record RatingTotals(int count, long sum) {

    /** The totals of a talk that nobody has rated. */
    public static final RatingTotals NONE = new RatingTotals(0, 0);
}
