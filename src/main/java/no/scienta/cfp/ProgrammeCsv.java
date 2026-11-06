package no.scienta.cfp;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Writes the programme as the print shop's CSV file (programme-export R4-R7): RFC 4180 with Unix line endings, the
 * fixed header line first, and a field quoted only when it has a comma, a quote or a line break in it. Every other
 * field is written as it is, also one that starts like a spreadsheet formula, as the request asks.
 */
public final class ProgrammeCsv {

    static final String HEADER = "slot,start,end,room,talkId,title,format,speakers";

    private static final String SPEAKER_SEPARATOR = "; ";

    private ProgrammeCsv() {
    }

    /** The whole file: the header and one line per row, each line ending in a newline. */
    public static String of(Programme programme) {
        return Stream.concat(Stream.of(HEADER), programme.rows().stream().map(ProgrammeCsv::line))
                .map(line -> line + "\n")
                .collect(Collectors.joining());
    }

    private static String line(ProgrammeRow row) {
        return Stream.of(row.slot().name(), row.start(), row.end(), row.room().name(), String.valueOf(row.talkId()),
                        row.title(), row.format(), String.join(SPEAKER_SEPARATOR, row.speakers()))
                .map(ProgrammeCsv::field)
                .collect(Collectors.joining(","));
    }

    /** The value as one CSV field: in quotes, with each quote doubled, only when it has to be (R6). */
    private static String field(String value) {
        return mustBeQuoted(value) ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }

    private static boolean mustBeQuoted(String value) {
        return List.of(",", "\"", "\n", "\r").stream().anyMatch(value::contains);
    }
}
