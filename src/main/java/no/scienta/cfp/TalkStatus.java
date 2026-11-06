package no.scienta.cfp;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Where a talk is in the acceptance state machine, and which moves between the statuses the rules allow
 * (Acceptance R1–R4, R8).
 */
public enum TalkStatus {
    SUBMITTED,
    ACCEPTED,
    REJECTED,
    WAITLISTED,
    WITHDRAWN;

    private static final Set<TalkStatus> DECISIONS = EnumSet.of(ACCEPTED, REJECTED, WAITLISTED);

    /** The statuses from which a talk can move to this status (R2, R4). */
    public Set<TalkStatus> reachableFrom() {
        switch (this) {
            case ACCEPTED:
            case REJECTED:
                return EnumSet.of(SUBMITTED, WAITLISTED);
            case WAITLISTED:
                return EnumSet.of(SUBMITTED);
            case WITHDRAWN:
                return EnumSet.of(SUBMITTED, WAITLISTED, ACCEPTED);
            default:
                return EnumSet.noneOf(TalkStatus.class);
        }
    }

    public boolean canMoveTo(TalkStatus next) {
        return next.reachableFrom().contains(this);
    }

    /** How many different reviewers must have rated a talk before it can move to this status (R3). */
    public int reviewersNeeded() {
        return this == ACCEPTED ? 2 : 0;
    }

    /** Whether a reviewer can still rate a talk with this status (R8). */
    public boolean isOpenForRating() {
        return this == SUBMITTED || this == WAITLISTED;
    }

    /** The committee's decision from a request body value: only the three decisions of R1. */
    public static Optional<TalkStatus> parseDecision(Object value) {
        return DECISIONS.stream().filter(decision -> decision.name().equals(value)).findFirst();
    }
}
