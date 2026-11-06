package no.scienta.cfp;

/**
 * The average rating the committee's statistics show.
 */
public final class RatingAverage {

    private RatingAverage() {
    }

    /** One decimal, cut off: 4.25 shows as 4.2. */
    public static double roundToOneDecimal(double average) {
        return Math.floor(average * 10) / 10.0;
    }
}
