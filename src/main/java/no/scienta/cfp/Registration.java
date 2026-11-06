package no.scienta.cfp;

/**
 * One attendee on a workshop's list (workshop-registration R8). The email is as the attendee typed it.
 */
public record Registration(long id, long workshopId, String name, String email) {
}
