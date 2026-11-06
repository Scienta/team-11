package no.scienta.cfp;

/**
 * What became of a request to unregister from a workshop (workshop-registration R9): the seat is free, or there was no
 * such registration under this workshop.
 */
public sealed interface UnregistrationOutcome {

    /** The registration is gone and its seat is free. */
    record Unregistered() implements UnregistrationOutcome {
    }

    /** The workshop does not exist, or has no registration with this ID. */
    record NoSuchRegistration() implements UnregistrationOutcome {
    }
}
