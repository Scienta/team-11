package no.scienta.cfp;

import java.util.Optional;

/** A room and a slot of the fixed venue: where one talk on the programme goes (scheduling R1, R5). */
public record Place(Room room, Slot slot) {

    /** The place from the path of a request, or empty when the venue has no such room or slot (R1). */
    public static Optional<Place> parse(String room, String slot) {
        return Room.parse(room).flatMap(knownRoom -> Slot.parse(slot).map(knownSlot -> new Place(knownRoom, knownSlot)));
    }
}
