package no.scienta.cfp;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Everything on the programme, in the order of the day: the earliest start first, and in one slot by room
 * (programme-export R1, R2). Both views of the export show these rows in this order.
 */
public record Programme(List<ProgrammeRow> rows) {

    private static final Comparator<ProgrammeRow> ORDER_OF_THE_DAY =
            Comparator.comparing(ProgrammeRow::slot).thenComparing(ProgrammeRow::room);

    public Programme {
        rows = rows.stream().sorted(ORDER_OF_THE_DAY).collect(Collectors.toUnmodifiableList());
    }
}
