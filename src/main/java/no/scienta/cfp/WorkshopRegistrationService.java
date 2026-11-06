package no.scienta.cfp;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers attendees for workshops and lists them (workshop-registration R1-R8). Returns the expected refusals as a
 * {@link RegistrationOutcome}; the rules themselves are in {@link WorkshopTalk}.
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
                .map(talk -> talk.refusal(dao.seatsTaken(workshopId), dao.emailRegistered(workshopId, request.email()))
                        .orElseGet(() -> new RegistrationOutcome.Registered(dao.insert(workshopId, request))))
                .orElseGet(RegistrationOutcome.NotAWorkshop::new);
    }

    /** The workshop's attendees in registration order, whatever its status (R4, R8), or empty when it is no workshop. */
    public Optional<List<Registration>> attendees(long workshopId) {
        return dao.findTalk(workshopId)
                .filter(WorkshopTalk::isWorkshop)
                .map(workshop -> dao.registrations(workshopId));
    }
}
