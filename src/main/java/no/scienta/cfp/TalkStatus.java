package no.scienta.cfp;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Where a talk is in the acceptance state machine, and which moves between the statuses the rules allow
 * (Acceptance R1–R4, R7, R8), and whether a workshop with this status takes registrations (Workshop registration
 * R3). Each status is declared after the statuses it can come from.
 */
public enum TalkStatus {
    SUBMITTED(Set.of(), 0, true, true, false),
    WAITLISTED(Set.of(SUBMITTED), 0, true, true, false),
    ACCEPTED(Set.of(SUBMITTED, WAITLISTED), 2, false, true, true),
    REJECTED(Set.of(SUBMITTED, WAITLISTED), 0, false, true, false),
    WITHDRAWN(Set.of(SUBMITTED, WAITLISTED, ACCEPTED), 0, false, false, false);

    private static final Set<TalkStatus> DECISIONS = EnumSet.of(ACCEPTED, REJECTED, WAITLISTED);

    private final Set<TalkStatus> reachableFrom;
    private final int reviewersNeeded;
    private final boolean openForRating;
    private final boolean countsTowardLimit;
    private final boolean takesRegistrations;

    TalkStatus(Set<TalkStatus> reachableFrom, int reviewersNeeded, boolean openForRating, boolean countsTowardLimit,
               boolean takesRegistrations) {
        this.reachableFrom = reachableFrom;
        this.reviewersNeeded = reviewersNeeded;
        this.openForRating = openForRating;
        this.countsTowardLimit = countsTowardLimit;
        this.takesRegistrations = takesRegistrations;
    }

    /** The statuses from which a talk can move to this status (R2, R4). */
    public Set<TalkStatus> reachableFrom() {
        return reachableFrom;
    }

    public boolean canMoveTo(TalkStatus next) {
        return next.reachableFrom().contains(this);
    }

    /** How many different reviewers must have rated a talk before it can move to this status (R3). */
    public int reviewersNeeded() {
        return reviewersNeeded;
    }

    /** Whether a reviewer can still rate a talk with this status (R8). */
    public boolean isOpenForRating() {
        return openForRating;
    }

    /** Whether a talk with this status counts towards the speaker's limit of talks (R7). */
    public boolean countsTowardLimit() {
        return countsTowardLimit;
    }

    /** Whether a workshop with this status takes new registrations (Workshop registration R3). */
    public boolean takesRegistrations() {
        return takesRegistrations;
    }

    /** The committee's decision from a request body value: only the three decisions of R1. */
    public static Optional<TalkStatus> parseDecision(Object value) {
        return DECISIONS.stream().filter(decision -> decision.name().equals(value)).findFirst();
    }
}
