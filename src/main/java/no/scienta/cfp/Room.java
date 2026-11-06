package no.scienta.cfp;

import java.util.Arrays;
import java.util.Optional;

/** A room of the fixed venue (scheduling R1). Nobody can add or change a room. */
public enum Room {
    R1, R2, R3;

    /** The room with exactly this name, in capitals, or empty when the venue has no such room. */
    public static Optional<Room> parse(String name) {
        return Arrays.stream(values()).filter(room -> room.name().equals(name)).findFirst();
    }
}
