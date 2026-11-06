package no.scienta.cfp;

/**
 * The formats a speaker can submit. The format sets the talk's duration.
 */
public enum TalkFormat {

    LIGHTNING_10(10),
    LIGHTNING_20(20),
    PRESENTATION(45),
    WORKSHOP(120);

    private final int durationMinutes;

    TalkFormat(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int durationMinutes() {
        return durationMinutes;
    }
}
