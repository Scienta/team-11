package no.scienta.cfp;

/**
 * What became of a request to put a talk on the programme: it is there, or one of the refusals that the rules expect
 * (scheduling R2-R6).
 */
public sealed interface ScheduleOutcome {

    /** The talk is in the room and slot, newly put there or there already. */
    record Scheduled(ProgrammeEntry entry) implements ScheduleOutcome {
    }

    /** Every outcome in which the programme does not change. */
    sealed interface Refusal extends ScheduleOutcome {
    }

    /** No talk has the ID in the request. */
    record NoSuchTalk() implements Refusal {
    }

    /** Only an accepted talk goes on the programme (R3). */
    record NotAccepted(TalkStatus status) implements Refusal {
    }

    /** The talk's format does not fit the slot: a workshop needs the long slot (R4). */
    record DoesNotFit(String format, Slot slot) implements Refusal {
    }

    /** Another talk is in the room and slot already (R5). */
    record PlaceTaken() implements Refusal {
    }

    /** The talk is on the programme in another room or slot already (R5). */
    record AlreadyScheduled() implements Refusal {
    }

    /** A speaker of the talk speaks in another room in the same slot (R6). */
    record SpeakerBusy() implements Refusal {
    }
}
