package no.scienta.cfp;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/** The talks in one slot, in all rooms (scheduling R5, R6). */
public record SlotProgramme(List<ScheduledTalk> talks) {

    public SlotProgramme {
        talks = List.copyOf(talks);
    }

    /** Whether a talk is in the room in this slot already (R5). */
    public boolean hasTalkIn(Room room) {
        return talks.stream().anyMatch(talk -> talk.room() == room);
    }

    /** Whether one of the speakers speaks in a talk in this slot already (R6). A speaker is one speaker ID. */
    public boolean hasSpeakerOf(Set<Long> speakerIds) {
        return talks.stream().anyMatch(talk -> !Collections.disjoint(talk.speakerIds(), speakerIds));
    }
}
