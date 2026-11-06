package no.scienta.cfp;

import java.util.List;

/**
 * What a request for a workshop's attendees finds (workshop-registration R4, R8): the list, or no workshop with this
 * ID.
 */
public sealed interface AttendeesOutcome {

    /** The workshop's attendees in the order they registered. */
    record Attendees(List<Registration> registrations) implements AttendeesOutcome {

        public Attendees {
            registrations = List.copyOf(registrations);
        }
    }

    /** No talk with this ID is a workshop (R2). */
    record NotAWorkshop() implements AttendeesOutcome {
    }
}
