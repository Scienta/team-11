package no.scienta.cfp;

import java.util.Set;

/** A talk on the programme as the rules for its slot see it: its room and all its speakers (scheduling R5, R6). */
public record ScheduledTalk(Room room, long talkId, Set<Long> speakerIds) {

    public ScheduledTalk {
        speakerIds = Set.copyOf(speakerIds);
    }
}
