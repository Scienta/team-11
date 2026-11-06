package no.scienta.cfp;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The average rating the committee's statistics show.
 */
public final class RatingAverage {

    private RatingAverage() {
    }

    /** One decimal, halves up: 4.25 shows as 4.3, as a talk's average does (Ratings R4). */
    public static double roundToOneDecimal(double average) {
        return BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
