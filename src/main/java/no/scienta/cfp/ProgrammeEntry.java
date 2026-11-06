package no.scienta.cfp;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** One talk on the programme, in its room and slot, with the slot's times as on the signs (scheduling R1, R2). */
@JsonPropertyOrder({"room", "slot", "start", "end", "talkId"})
public record ProgrammeEntry(Room room, Slot slot, long talkId) {

    public ProgrammeEntry {
        Objects.requireNonNull(room, "room");
        Objects.requireNonNull(slot, "slot");
    }

    public static ProgrammeEntry at(Place place, long talkId) {
        return new ProgrammeEntry(place.room(), place.slot(), talkId);
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
