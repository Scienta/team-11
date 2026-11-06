package no.scienta.cfp;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Puts talks on the programme, looks up what is in a place, and takes talks out (scheduling R1-R8). Returns the
 * expected refusals as a {@link ScheduleOutcome}; the rules themselves are in {@link ProgrammeTalk}.
 */
@Service
public class ProgrammeService {

    private final ProgrammeDao dao;

    public ProgrammeService(ProgrammeDao dao) {
        this.dao = dao;
    }

    /**
     * Puts the talk in the place if the rules allow it. The slot stays locked in all rooms, and the talk against
     * changes, until the entry is saved (R8).
     */
    @Transactional
    public ScheduleOutcome schedule(Place place, SchedulingRequest request) {
        dao.lockSlot(place.slot());
        return dao.lockTalk(request.talkId())
                .map(talk -> scheduleIfAllowed(place, talk))
                .orElseGet(ScheduleOutcome.NoSuchTalk::new);
    }

    /** Puts the talk in the place unless the rules refuse it. Runs under the locks that {@link #schedule} takes. */
    private ScheduleOutcome scheduleIfAllowed(Place place, ProgrammeTalk talk) {
        Optional<Place> scheduledAt = dao.placeOf(talk.id());
        return talk.refusal(place, dao.talksIn(place.slot()), scheduledAt)
                .<ScheduleOutcome>map(refusal -> refusal)
                .orElseGet(() -> addUnlessThere(place, talk, scheduledAt));
    }

    /**
     * Saves the entry, unless the talk is in the place already from the same request before. The unique index on the
     * talk refuses the entry when the same talk went into another slot at the same time (R5, R8).
     */
    private ScheduleOutcome addUnlessThere(Place place, ProgrammeTalk talk, Optional<Place> scheduledAt) {
        if (scheduledAt.isPresent() || dao.add(place, talk.id())) {
            return new ScheduleOutcome.Scheduled(ProgrammeEntry.at(place, talk.id()));
        }
        return new ScheduleOutcome.AlreadyScheduled();
    }

    /** The talk in the place, if there is one, whatever has happened to the talk since (R2, R7). */
    @Transactional(readOnly = true)
    public Optional<ProgrammeEntry> entryAt(Place place) {
        return dao.entryAt(place);
    }

    /** Takes the talk out of the place, whatever its status now (R2, R7). */
    @Transactional
    public TakeOutOutcome takeOut(Place place) {
        dao.lockSlot(place.slot());
        return dao.remove(place) ? new TakeOutOutcome.TakenOut() : new TakeOutOutcome.NothingThere();
    }
}
