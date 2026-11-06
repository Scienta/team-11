package no.scienta.cfp;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The ratings of one talk as the talk lists show them: how many reviewers rated it, the sum of their scores, and the
 * lowest and highest score.
 */
public record RatingTotals(int count, long sum, int lowest, int highest) {

    /** The totals of a talk that nobody has rated. */
    public static final RatingTotals NONE = new RatingTotals(0, 0, 0, 0);

    /** From this many ratings on, the average leaves out one highest and one lowest score (ratings-cr R2). */
    private static final int TRIMMED_FROM = 5;

    /**
     * The exact mean of the scores that count, rounded to 2 decimals, ties to even (ratings-cr R1–R3). No average
     * without ratings (R6).
     */
    public Double average() {
        if (count == 0) {
            return null;
        }
        boolean trimmed = count >= TRIMMED_FROM;
        long counted = trimmed ? sum - highest - lowest : sum;
        int reviewers = trimmed ? count - 2 : count;
        return BigDecimal.valueOf(counted).divide(BigDecimal.valueOf(reviewers), 2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
