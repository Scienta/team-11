package no.scienta.cfp;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;

/**
 * A time slot, the same in every room of the venue (scheduling R1), and which talks fit it (R4). No two slots
 * overlap, so "at the same time" means "in the same slot" (R6).
 */
public enum Slot {
    S1(LocalTime.of(9, 0), LocalTime.of(10, 0), false),
    S2(LocalTime.of(10, 15), LocalTime.of(11, 15), false),
    S3(LocalTime.of(11, 30), LocalTime.of(12, 30), false),
    S4(LocalTime.of(13, 30), LocalTime.of(15, 30), true),
    S5(LocalTime.of(15, 45), LocalTime.of(16, 45), false);

    private static final DateTimeFormatter AS_ON_THE_SIGNS = DateTimeFormatter.ofPattern("HH:mm");

    private final LocalTime start;
    private final LocalTime end;
    private final boolean longSlot;

    Slot(LocalTime start, LocalTime end, boolean longSlot) {
        this.start = start;
        this.end = end;
        this.longSlot = longSlot;
    }

    /** The start as on the signs, {@code HH:mm} on a 24-hour clock (R1). */
    public String start() {
        return start.format(AS_ON_THE_SIGNS);
    }

    /** The end as on the signs, {@code HH:mm} on a 24-hour clock (R1). */
    public String end() {
        return end.format(AS_ON_THE_SIGNS);
    }

    /** Whether a talk of the format fits this slot: a workshop needs the long slot, every other format fits (R4). */
    public boolean fits(String format) {
        return longSlot || !"WORKSHOP".equals(format);
    }

    /** The slot with exactly this name, in capitals, or empty when the venue has no such slot. */
    public static Optional<Slot> parse(String name) {
        return Arrays.stream(values()).filter(slot -> slot.name().equals(name)).findFirst();
    }
}
