package no.scienta.cfp;

/** What became of a request to take a talk out of a room and slot (scheduling R2, R7). */
public sealed interface TakeOutOutcome {

    /** The talk is out of the place, and the place is empty. */
    record TakenOut() implements TakeOutOutcome {
    }

    /** No talk was in the place. */
    record NothingThere() implements TakeOutOutcome {
    }
}
