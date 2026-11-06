package no.scienta.cfp;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * A talk as the programme sees it: its format, status and all its speakers. Decides whether the talk can go in a
 * place (scheduling R3-R6), as a pure function of what the database holds when the talk is put on the programme (R7).
 */
public record ProgrammeTalk(long id, String format, TalkStatus status, Set<Long> speakerIds) {

    public ProgrammeTalk {
        speakerIds = Set.copyOf(speakerIds);
    }

    /** The talk with its main speaker and co-speakers as speakers. */
    public static ProgrammeTalk of(TalkDto talk) {
        Set<Long> speakers = new HashSet<>(talk.coSpeakerIds);
        speakers.add(talk.speakerId);
        return new ProgrammeTalk(talk.id, talk.format, talk.status, speakers);
    }

    /**
     * Why the talk cannot go in the place, or empty when it can. Empty also when the talk is in the place already, so
     * that the same request twice does not fail. The rules are checked in the order of "Our decisions" in the spec.
     *
     * @param slotProgramme the talks in the place's slot, in all rooms
     * @param scheduledAt   where the talk is on the programme now, if it is
     */
    public Optional<ScheduleOutcome.Refusal> refusal(Place place, SlotProgramme slotProgramme,
                                                     Optional<Place> scheduledAt) {
        if (!status.goesOnProgramme()) {
            return Optional.of(new ScheduleOutcome.NotAccepted(status));
        }
        if (!place.slot().fits(format)) {
            return Optional.of(new ScheduleOutcome.DoesNotFit(format, place.slot()));
        }
        if (scheduledAt.filter(place::equals).isPresent()) {
            return Optional.empty();
        }
        if (slotProgramme.hasTalkIn(place.room())) {
            return Optional.of(new ScheduleOutcome.PlaceTaken());
        }
        if (scheduledAt.isPresent()) {
            return Optional.of(new ScheduleOutcome.AlreadyScheduled());
        }
        if (slotProgramme.hasSpeakerOf(speakerIds)) {
            return Optional.of(new ScheduleOutcome.SpeakerBusy());
        }
        return Optional.empty();
    }
}
