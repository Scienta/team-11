package no.scienta.cfp;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * How many reviewers rated a talk, and their average score: the exact mean of the scores that count, rounded to 2
 * decimals, ties to even (ratings-cr R1–R3). No average without ratings (R6).
 */
public record RatingSummary(int count, Double average) {

    static final RatingSummary NONE = new RatingSummary(0, null);

    /** From this many ratings on, the average leaves out one highest and one lowest score (ratings-cr R2). */
    private static final int TRIMMED_FROM = 5;

    static RatingSummary of(int count, long sum, int lowest, int highest) {
        return new RatingSummary(count, count == 0 ? null : average(count, sum, lowest, highest));
    }

    private static double average(int count, long sum, int lowest, int highest) {
        boolean trimmed = count >= TRIMMED_FROM;
        long counted = trimmed ? sum - highest - lowest : sum;
        int reviewers = trimmed ? count - 2 : count;
        return BigDecimal.valueOf(counted).divide(BigDecimal.valueOf(reviewers), 2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
