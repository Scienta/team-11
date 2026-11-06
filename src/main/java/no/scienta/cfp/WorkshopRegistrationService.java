package no.scienta.cfp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers attendees for workshops, unregisters them, and lists them (workshop-registration R1-R10). Returns the
 * expected refusals as a {@link RegistrationOutcome}; the rules themselves are in {@link WorkshopTalk}.
 */
@Service
public class WorkshopRegistrationService {

    private final WorkshopRegistrationDao dao;

    public WorkshopRegistrationService(WorkshopRegistrationDao dao) {
        this.dao = dao;
    }

    /** Registers the attendee if the workshop takes them. The talk row stays locked until the registration is saved. */
    @Transactional
    public RegistrationOutcome register(long workshopId, RegistrationRequest request) {
        return dao.lockTalk(workshopId)
                .map(talk -> registerIfTaken(talk, workshopId, request))
                .orElseGet(RegistrationOutcome.NotAWorkshop::new);
    }

    /** Registers the attendee unless the talk refuses them. Runs under the row lock that {@link #register} takes. */
    private RegistrationOutcome registerIfTaken(WorkshopTalk talk, long workshopId, RegistrationRequest request) {
        return talk.refusal(dao.seatsTaken(workshopId), dao.emailRegistered(workshopId, request.email()))
                .orElseGet(() -> dao.addAttendee(workshopId, request)
                        .<RegistrationOutcome>map(RegistrationOutcome.Registered::new)
                        .orElseGet(RegistrationOutcome.AlreadyRegistered::new));
    }

    /**
     * Unregisters the attendee and frees the seat (R9), if the email is the registration's email (R10). Takes the same
     * talk row lock as {@link #register}, so a freed seat goes to one registration only.
     */
    @Transactional
    public UnregistrationOutcome unregister(long workshopId, long registrationId, String email) {
        boolean deleted = dao.lockTalk(workshopId)
                .filter(WorkshopTalk::isWorkshop)
                .map(workshop -> dao.delete(workshopId, registrationId, email))
                .orElse(false);
        return deleted ? new UnregistrationOutcome.Unregistered() : new UnregistrationOutcome.NoSuchRegistration();
    }

    /** The workshop's attendees in registration order, whatever its status (R4, R8). */
    @Transactional(readOnly = true)
    public AttendeesOutcome attendees(long workshopId) {
        return dao.findTalk(workshopId)
                .filter(WorkshopTalk::isWorkshop)
                .<AttendeesOutcome>map(workshop -> new AttendeesOutcome.Attendees(dao.registrations(workshopId)))
                .orElseGet(AttendeesOutcome.NotAWorkshop::new);
    }
}
