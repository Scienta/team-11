package no.scienta.cfp;

/**
 * What a requested status change did to a talk: it moved, or one of the reasons why it did not (Acceptance R2–R4).
 */
public sealed interface StatusChange {

    /** The talk moved to the new status. The talk is as the API shows it, ratings included. */
    record Moved(TalkDto talk) implements StatusChange {
    }

    /** No talk has this id. */
    record NotFound() implements StatusChange {
    }

    /** Somebody other than the main speaker asked to withdraw the talk (R4). */
    record NotMainSpeaker() implements StatusChange {
    }

    /** The rules do not allow the move from the current status (R2, R4). */
    record NotAllowed(TalkStatus current, TalkStatus next) implements StatusChange {
    }

    /** The move is allowed, but too few different reviewers have rated the talk (R3). */
    record NeedsReviewers(TalkStatus next) implements StatusChange {
    }
}
