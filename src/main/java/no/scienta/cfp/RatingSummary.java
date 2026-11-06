package no.scienta.cfp;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * How many reviewers rated a talk, and their average score with one decimal, halves up (4.25 shows as 4.3). No
 * average without ratings.
 */
public record RatingSummary(int count, Double average) {

    static final RatingSummary NONE = new RatingSummary(0, null);

    static RatingSummary of(int count, long total) {
        return new RatingSummary(count, count == 0 ? null
                : BigDecimal.valueOf(total).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP).doubleValue());
    }
}
