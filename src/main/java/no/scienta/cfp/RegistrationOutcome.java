package no.scienta.cfp;

/**
 * What became of a request to register for a workshop: registered, or one of the refusals that the rules expect
 * (workshop-registration R2, R3, R5, R6).
 */
public sealed interface RegistrationOutcome {

    /** The attendee is on the list. */
    record Registered(Registration registration) implements RegistrationOutcome {
    }

    /** Every outcome in which the attendee is not on the list. */
    sealed interface Refusal extends RegistrationOutcome {
    }

    /** No talk with this ID is a workshop (R2). */
    record NotAWorkshop() implements Refusal {
    }

    /** The workshop is not accepted, so it takes no registrations (R3). */
    record Closed(TalkStatus status) implements Refusal {
    }

    /** Every seat is taken (R5). */
    record Full(int seats) implements Refusal {
    }

    /** This email is already on the workshop's list, in some letter case (R6). */
    record AlreadyRegistered() implements Refusal {
    }
}
