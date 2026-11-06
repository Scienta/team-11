package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.junit.TypeConverter;

/**
 * The programme as the print shop's CSV file, and the order of its rows (programme-export R2, R4-R7). Builds the rows
 * in memory: no database and no HTTP. In the tables, {@code ⏎} is a newline and {@code ␍} a carriage return.
 */
@DisplayName("Programme export: the CSV file")
public class ProgrammeCsvTest {

    @DisplayName("R5, R6, R7: Writes the header, then one line per row")
    @Description("""
            Each place gets a talk of its own: the first place talk 1, the next talk 2. Each talk is a
            PRESENTATION by Grace Hopper with the title Talk and its ID.
            """)
    @TableTest("""
        Scenario        | Places         | Body?
        Empty programme | []             | slot,start,end,room,talkId,title,format,speakers⏎
        One talk        | [R1/S1]        | slot,start,end,room,talkId,title,format,speakers⏎S1,09:00,10:00,R1,1,Talk 1,PRESENTATION,Grace Hopper⏎
        Two talks       | [R1/S1, R2/S4] | slot,start,end,room,talkId,title,format,speakers⏎S1,09:00,10:00,R1,1,Talk 1,PRESENTATION,Grace Hopper⏎S4,13:30,15:30,R2,2,Talk 2,PRESENTATION,Grace Hopper⏎
        """)
    void writesTheHeaderThenOneLinePerRow(List<Place> places, Text body) {
        assertEquals(body.value(), ProgrammeCsv.of(new Programme(talksIn(places))));
    }

    @DisplayName("R2: Orders the rows by start, then by room")
    @TableTest("""
        Scenario                       | Places In Any Order          | Order?
        Three rooms, then a later slot | [R3/S1, R1/S2, R1/S1, R2/S1] | [R1/S1, R2/S1, R3/S1, R1/S2]
        Long slot before the last      | [R1/S5, R3/S4, R2/S4]        | [R2/S4, R3/S4, R1/S5]
        """)
    void ordersTheRowsByStartThenByRoom(List<Place> placesInAnyOrder, List<Place> order) {
        List<Place> listed = new Programme(talksIn(placesInAnyOrder)).rows().stream()
                .map(row -> new Place(row.room(), row.slot()))
                .collect(Collectors.toList());
        assertEquals(order, listed);
    }

    @DisplayName("R6: Quotes a field only when it has to")
    @Description("""
            Each row is talk 7, a PRESENTATION in R1 S1. A title that starts like a spreadsheet formula
            is written as it is: the request asks for every other field as it is.
            """)
    @TableTest("""
        Scenario                 | Title                   | Main Speaker             | CSV Line?
        Plain title              | Legacy is a feeling     | Grace Hopper             | S1,09:00,10:00,R1,7,Legacy is a feeling,PRESENTATION,Grace Hopper
        Comma in the title       | Kotlin, really fast     | Grace Hopper             | 'S1,09:00,10:00,R1,7,"Kotlin, really fast",PRESENTATION,Grace Hopper'
        Comma and quotes         | 'Kotlin, "really" fast' | Grace Hopper             | 'S1,09:00,10:00,R1,7,"Kotlin, ""really"" fast",PRESENTATION,Grace Hopper'
        Quotes only              | 'Say "hi"'              | Grace Hopper             | 'S1,09:00,10:00,R1,7,"Say ""hi""\",PRESENTATION,Grace Hopper'
        Newline in the title     | Two⏎lines               | Grace Hopper             | 'S1,09:00,10:00,R1,7,"Two⏎lines",PRESENTATION,Grace Hopper'
        Carriage return          | Two␍⏎lines              | Grace Hopper             | 'S1,09:00,10:00,R1,7,"Two␍⏎lines",PRESENTATION,Grace Hopper'
        Starts like a formula    | =SUM(1;2)               | Grace Hopper             | S1,09:00,10:00,R1,7,=SUM(1;2),PRESENTATION,Grace Hopper
        Spaces around the title  | '  Spaces around  '     | Grace Hopper             | 'S1,09:00,10:00,R1,7,  Spaces around  ,PRESENTATION,Grace Hopper'
        Comma in a speaker name  | Legacy is a feeling     | Hopper, Grace            | 'S1,09:00,10:00,R1,7,Legacy is a feeling,PRESENTATION,"Hopper, Grace"'
        Quotes in a speaker name | Legacy is a feeling     | 'Grace "Amazing" Hopper' | 'S1,09:00,10:00,R1,7,Legacy is a feeling,PRESENTATION,"Grace ""Amazing"" Hopper"'
        """)
    void quotesAFieldOnlyWhenItHasTo(Text title, String mainSpeaker, Text csvLine) {
        ProgrammeRow row = new ProgrammeRow(Slot.S1, Room.R1, 7, title.value(), "PRESENTATION", List.of(mainSpeaker));
        assertEquals(header() + csvLine.value() + "\n", ProgrammeCsv.of(new Programme(List.of(row))));
    }

    @DisplayName("R4: Joins the speakers with a semicolon in one field")
    @Description("""
            Each row is talk 7, "Legacy is a feeling", a PRESENTATION in R1 S1. The speakers come in the
            order of the list: the main speaker first, then the co-speakers in the order they were added.
            """)
    @TableTest("""
        Scenario          | Speakers                                     | CSV Line?
        Main speaker only | [Grace Hopper]                               | S1,09:00,10:00,R1,7,Legacy is a feeling,PRESENTATION,Grace Hopper
        One co-speaker    | [Grace Hopper, Ada Lovelace]                 | S1,09:00,10:00,R1,7,Legacy is a feeling,PRESENTATION,Grace Hopper; Ada Lovelace
        Two co-speakers   | [Grace Hopper, Barbara Liskov, Ada Lovelace] | S1,09:00,10:00,R1,7,Legacy is a feeling,PRESENTATION,Grace Hopper; Barbara Liskov; Ada Lovelace
        """)
    void joinsTheSpeakersWithASemicolonInOneField(List<String> speakers, String csvLine) {
        ProgrammeRow row = new ProgrammeRow(Slot.S1, Room.R1, 7, "Legacy is a feeling", "PRESENTATION", speakers);
        assertEquals(header() + csvLine + "\n", ProgrammeCsv.of(new Programme(List.of(row))));
    }

    // ------------------------------------------------------------------------------------------- table types

    /** A text from a cell, with {@code ⏎} for a newline and {@code ␍} for a carriage return. */
    record Text(String value) {
    }

    @TypeConverter
    public static Text parseText(String cell) {
        return new Text(cell.replace("⏎", "\n").replace("␍", "\r"));
    }

    /** A place written as {@code R1/S1}. */
    @TypeConverter
    public static Place parsePlace(String cell) {
        String[] parts = cell.trim().split("/");
        return Place.parse(parts[0], parts[1]).orElseThrow();
    }

    // ------------------------------------------------------------------------------------------- fixture

    private static List<ProgrammeRow> talksIn(List<Place> places) {
        return IntStream.range(0, places.size())
                .mapToObj(index -> new ProgrammeRow(places.get(index).slot(), places.get(index).room(), index + 1,
                        "Talk " + (index + 1), "PRESENTATION", List.of("Grace Hopper")))
                .collect(Collectors.toList());
    }

    private static String header() {
        return "slot,start,end,room,talkId,title,format,speakers\n";
    }
}
