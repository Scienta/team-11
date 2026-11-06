package no.scienta.cfp;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * One talk on the programme as the export shows it: the slot and its times, the room, and the talk with its speakers as
 * they are now (programme-export R3, R4). The speakers are names, the main speaker first.
 */
@JsonPropertyOrder({"slot", "start", "end", "room", "talkId", "title", "format", "speakers"})
public record ProgrammeRow(Slot slot, Room room, long talkId, String title, String format, List<String> speakers) {

    public ProgrammeRow {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(room, "room");
        speakers = List.copyOf(speakers);
    }

    @JsonProperty("start")
    public String start() {
        return slot.start();
    }

    @JsonProperty("end")
    public String end() {
        return slot.end();
    }
}
