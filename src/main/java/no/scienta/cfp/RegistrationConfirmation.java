package no.scienta.cfp;

/**
 * The answer to a new registration: the attendee on the list, and the cancellation token that only this answer shows
 * (workshop-registration R8, R10).
 */
public record RegistrationConfirmation(long id, long workshopId, String name, String email, String cancellationToken) {

    public static RegistrationConfirmation of(Registration registration, CancellationToken token) {
        return new RegistrationConfirmation(registration.id(), registration.workshopId(), registration.name(),
                registration.email(), token.value());
    }
}
