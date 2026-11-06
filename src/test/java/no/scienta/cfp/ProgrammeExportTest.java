package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.junit.TypeConverter;

/**
 * The programme export through HTTP: which talks the two views list, what a row shows, and how the CSV is served
 * (programme-export R1-R7). Runs the real app on a random port with a throwaway database, emptied before each row, so
 * the first talk of a row gets ID 1. A talk in a table is a letter; the fixture creates it the first time a row names
 * it, as an accepted presentation by Grace Hopper titled "Legacy is a feeling".
 */
@DisplayName("Programme export")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProgrammeExportTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HEADER = "slot,start,end,room,talkId,title,format,speakers";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();

    /** The talk letters of a row, and the IDs the fixture created them with. */
    private final Map<String, Long> talks = new HashMap<>();

    /** The speaker names of a row, and the IDs the fixture registered them with. */
    private final Map<String, Long> speakers = new HashMap<>();

    @BeforeEach
    void startWithAnEmptyProgramme() {
        jdbc.execute("TRUNCATE speaker, talk, rating, talk_co_speaker, programme_entry RESTART IDENTITY");
    }

    @DisplayName("R1, R2, R7: Lists only the talks on the programme")
    @Description("""
            The programme is set up with PUT, in the order of the cell. The other talks are not on the
            programme: an accepted talk, a submitted talk, or a talk taken out again. The talks are
            read from the JSON list and from the talkId column of the CSV.
            """)
    @TableTest("""
        Scenario                   | Programme            | Other Talks                               | JSON Talks? | CSV Talks?
        Two talks                  | [R1/S1: A, R2/S3: B] | [:]                                       | [A, B]      | [A, B]
        Put in a later slot first  | [R2/S3: B, R1/S1: A] | [:]                                       | [A, B]      | [A, B]
        Empty programme            | [:]                  | [:]                                       | []          | []
        Talks not on the programme | [:]                  | [C: accepted, D: submitted, E: taken out] | []          | []
        One of two accepted talks  | [R1/S1: A]           | [C: accepted]                             | [A]         | [A]
        """)
    void listsOnlyTheTalksOnTheProgramme(Map<String, String> programme, Map<String, String> otherTalks,
                                         List<String> jsonTalks, List<String> csvTalks) throws Exception {
        schedule(programme);
        otherTalks.forEach(this::createOtherTalk);
        assertEquals(jsonTalks, lettersOf(jsonRows()));
        assertEquals(csvTalks, lettersOfCsv(csvLines()));
    }

    @DisplayName("R3: Shows each talk as it is now")
    @Description("""
            Talk A is talk 1, by Grace Hopper, and the change happens after it is on the programme. The
            JSON row and the CSV line are compared as text.
            """)
    @TableTest("""
        Scenario                      | Title               | Format       | Co-speakers    | Place | Change               | JSON Row?                                                                                                                                                             | CSV Line?
        A presentation                | Legacy is a feeling | PRESENTATION | []             | R1/S1 |                      | '{"slot":"S1","start":"09:00","end":"10:00","room":"R1","talkId":1,"title":"Legacy is a feeling","format":"PRESENTATION","speakers":["Grace Hopper"]}'                | S1,09:00,10:00,R1,1,Legacy is a feeling,PRESENTATION,Grace Hopper
        A workshop                    | Hands on            | WORKSHOP     | []             | R2/S4 |                      | '{"slot":"S4","start":"13:30","end":"15:30","room":"R2","talkId":1,"title":"Hands on","format":"WORKSHOP","speakers":["Grace Hopper"]}'                               | S4,13:30,15:30,R2,1,Hands on,WORKSHOP,Grace Hopper
        Withdrawn afterwards          | Legacy is a feeling | PRESENTATION | []             | R3/S5 | withdrawn            | '{"slot":"S5","start":"15:45","end":"16:45","room":"R3","talkId":1,"title":"Legacy is a feeling","format":"PRESENTATION","speakers":["Grace Hopper"]}'                | S5,15:45,16:45,R3,1,Legacy is a feeling,PRESENTATION,Grace Hopper
        Co-speaker added afterwards   | Legacy is a feeling | PRESENTATION | []             | R1/S2 | Ada Lovelace added   | '{"slot":"S2","start":"10:15","end":"11:15","room":"R1","talkId":1,"title":"Legacy is a feeling","format":"PRESENTATION","speakers":["Grace Hopper","Ada Lovelace"]}' | S2,10:15,11:15,R1,1,Legacy is a feeling,PRESENTATION,Grace Hopper; Ada Lovelace
        Co-speaker removed afterwards | Legacy is a feeling | PRESENTATION | [Ada Lovelace] | R1/S3 | Ada Lovelace removed | '{"slot":"S3","start":"11:30","end":"12:30","room":"R1","talkId":1,"title":"Legacy is a feeling","format":"PRESENTATION","speakers":["Grace Hopper"]}'                | S3,11:30,12:30,R1,1,Legacy is a feeling,PRESENTATION,Grace Hopper
        """)
    void showsEachTalkAsItIsNow(String title, String format, List<String> coSpeakers, String place, Change change,
                                String jsonRow, String csvLine) throws Exception {
        long talk = createTalk("A", title, format, "Grace Hopper", coSpeakers);
        schedule(Map.of(place, "A"));
        apply(change, talk);
        assertEquals(List.of(jsonRow), jsonRows().stream().map(JsonNode::toString).collect(Collectors.toList()));
        assertEquals(List.of(csvLine), csvLines());
    }

    @DisplayName("R4: Names the main speaker, then the co-speakers as added")
    @Description("""
            Talk A is put in R1 S1. Its co-speakers are added in the order of the list, before A is
            accepted. Ada Lovelace, Barbara Liskov and Grace Hopper are registered speakers.
            """)
    @TableTest("""
        Scenario                | Main Speaker | Co-speakers In Order Added     | Speakers?
        Main speaker only       | Grace Hopper | []                             | [Grace Hopper]
        One co-speaker          | Grace Hopper | [Ada Lovelace]                 | [Grace Hopper, Ada Lovelace]
        Not in alphabetic order | Grace Hopper | [Barbara Liskov, Ada Lovelace] | [Grace Hopper, Barbara Liskov, Ada Lovelace]
        """)
    void namesTheMainSpeakerThenTheCoSpeakersAsAdded(String mainSpeaker, List<String> coSpeakersInOrderAdded,
                                                     List<String> speakersListed) throws Exception {
        createTalk("A", "Legacy is a feeling", "PRESENTATION", mainSpeaker, coSpeakersInOrderAdded);
        schedule(Map.of("R1/S1", "A"));
        assertEquals(List.of(speakersListed), jsonRows().stream().map(ProgrammeExportTest::speakersOf)
                .collect(Collectors.toList()));
        assertEquals(List.of(String.join("; ", speakersListed)), csvLines().stream()
                .map(line -> line.substring(line.lastIndexOf(',') + 1)).collect(Collectors.toList()));
    }

    @DisplayName("R5: Serves each view in its own type, whatever the client asks for")
    @Description("""
            The programme is empty. A blank Accept sends no Accept header. The body is read as UTF-8
            bytes, so a byte order mark in front of the header would show.
            """)
    @TableTest("""
        Scenario              | Path           | Accept                        | Status? | Content Type?                  | Body?
        CSV                   | /programme.csv | {'', text/csv, '*/*'}         | 200     | text/csv;charset=UTF-8         | slot,start,end,room,talkId,title,format,speakers⏎
        CSV for a JSON client | /programme.csv | application/json              | 200     | text/csv;charset=UTF-8         | slot,start,end,room,talkId,title,format,speakers⏎
        JSON                  | /programme     | {'', application/json, '*/*'} | 200     | application/json;charset=UTF-8 | []
        JSON for a CSV client | /programme     | text/csv                      | 200     | application/json;charset=UTF-8 | []
        """)
    void servesEachViewInItsOwnType(String path, String accept, int status, String contentType, Text body)
            throws Exception {
        HttpResponse<byte[]> answer = get(path, accept);
        assertEquals(status, answer.statusCode());
        assertEquals(contentType, answer.headers().firstValue("Content-Type").orElse(null));
        assertEquals(body.value(), new String(answer.body(), StandardCharsets.UTF_8));
    }

    @DisplayName("R5, R6: Writes a stored title into the CSV as the print shop needs it")
    @Description("""
            Talk A is talk 1, a presentation by Grace Hopper, in R1 S1. The CSV is read as UTF-8 bytes.
            In the cells, ⏎ is a newline.
            """)
    @TableTest("""
        Scenario                | Title                   | CSV Body?
        Letters outside ASCII   | Kåre og Åse 🚀          | slot,start,end,room,talkId,title,format,speakers⏎S1,09:00,10:00,R1,1,Kåre og Åse 🚀,PRESENTATION,Grace Hopper⏎
        Comma and quotes        | 'Kotlin, "really" fast' | 'slot,start,end,room,talkId,title,format,speakers⏎S1,09:00,10:00,R1,1,"Kotlin, ""really"" fast",PRESENTATION,Grace Hopper⏎'
        Line break in the title | Two⏎lines               | 'slot,start,end,room,talkId,title,format,speakers⏎S1,09:00,10:00,R1,1,"Two⏎lines",PRESENTATION,Grace Hopper⏎'
        """)
    void writesAStoredTitleIntoTheCsv(Text title, Text csvBody) throws Exception {
        createTalk("A", title.value(), "PRESENTATION", "Grace Hopper", List.of());
        schedule(Map.of("R1/S1", "A"));
        assertEquals(csvBody.value(), new String(get("/programme.csv", "").body(), StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------------------------------- table types

    /** A change to a talk after it is on the programme: {@code withdrawn}, {@code Ada Lovelace added} or removed. */
    record Change(String text) {
    }

    @TypeConverter
    public static Change parseChange(String cell) {
        return new Change(cell.trim());
    }

    /** A text from a cell, with {@code ⏎} for a newline. */
    record Text(String value) {
    }

    @TypeConverter
    public static Text parseText(String cell) {
        return new Text(cell.replace("⏎", "\n"));
    }

    // ------------------------------------------------------------------------------------------- fixture

    /** The ID of the talk, created as an accepted presentation by Grace Hopper if the row has not got it yet. */
    private long talk(String letter) {
        if (!talks.containsKey(letter)) {
            createTalk(letter, "Legacy is a feeling", "PRESENTATION", "Grace Hopper", List.of());
        }
        return talks.get(letter);
    }

    /** Submits the talk, adds its co-speakers in order, and accepts it through the acceptance API. */
    private long createTalk(String letter, String title, String format, String mainSpeaker, List<String> coSpeakers) {
        long talk = submit(title, format, speaker(mainSpeaker));
        coSpeakers.forEach(co -> expect(200, send("PUT", "/talks/" + talk + "/co-speakers/" + speaker(co), null)));
        accept(talk);
        talks.put(letter, talk);
        return talk;
    }

    /** Creates a talk that is not on the programme, in the state of the cell. */
    private void createOtherTalk(String letter, String state) {
        switch (state) {
            case "accepted" -> talk(letter);
            case "submitted" -> talks.put(letter, submit("Legacy is a feeling", "PRESENTATION", speaker("Grace Hopper")));
            case "taken out" -> {
                expect(200, send("PUT", "/programme/R1/S2", "{\"talkId\": " + talk(letter) + "}"));
                expect(204, send("DELETE", "/programme/R1/S2", null));
            }
            default -> throw new IllegalArgumentException("no fixture for the state " + state);
        }
    }

    private long submit(String title, String format, long speaker) {
        ObjectNodeBuilder body = new ObjectNodeBuilder().put("title", title).put("format", format)
                .put("level", "BEGINNER").put("speakerId", speaker);
        return id(send("POST", "/talks", body.toString()));
    }

    private void accept(long talk) {
        IntStream.rangeClosed(1, 2).forEach(reviewer -> expect(200,
                send("PUT", "/talks/" + talk + "/ratings/" + reviewer, "{\"score\": 4}")));
        expect(200, send("PUT", "/talks/" + talk + "/status", "{\"status\": \"ACCEPTED\"}"));
    }

    private long speaker(String name) {
        return speakers.computeIfAbsent(name, newName -> id(send("POST", "/speakers",
                new ObjectNodeBuilder().put("name", newName)
                        .put("email", "speaker-" + speakers.size() + "@example.com").toString())));
    }

    /** Applies a change from the R3 table; a blank change does nothing. */
    private void apply(Change change, long talk) {
        if (change == null) {
            return;
        }
        if ("withdrawn".equals(change.text())) {
            expect(200, send("POST", "/talks/" + talk + "/withdraw", "{\"speakerId\": " + speaker("Grace Hopper") + "}"));
        } else if (change.text().endsWith(" added")) {
            expect(200, send("PUT", "/talks/" + talk + "/co-speakers/" + speaker(nameIn(change)), null));
        } else if (change.text().endsWith(" removed")) {
            expect(200, send("DELETE", "/talks/" + talk + "/co-speakers/" + speaker(nameIn(change)), null));
        } else {
            throw new IllegalArgumentException("no fixture for the change " + change.text());
        }
    }

    private static String nameIn(Change change) {
        return change.text().substring(0, change.text().lastIndexOf(' '));
    }

    /** Puts each talk in its place, one after the other, in the order of the map. */
    private void schedule(Map<String, String> programme) {
        programme.forEach((place, letter) ->
                expect(200, send("PUT", "/programme/" + place, "{\"talkId\": " + talk(letter) + "}")));
    }

    private List<JsonNode> jsonRows() throws Exception {
        HttpResponse<byte[]> answer = get("/programme", "");
        assertEquals(200, answer.statusCode());
        return StreamSupport.stream(JSON.readTree(answer.body()).spliterator(), false).collect(Collectors.toList());
    }

    /** The lines of the CSV after its header, read with a line break only between rows. */
    private List<String> csvLines() {
        HttpResponse<byte[]> answer = get("/programme.csv", "");
        assertEquals(200, answer.statusCode());
        String body = new String(answer.body(), StandardCharsets.UTF_8);
        List<String> lines = Arrays.asList(body.split("\n", -1));
        assertEquals(HEADER, lines.get(0));
        assertEquals("", lines.get(lines.size() - 1), "the body ends in a newline");
        return lines.subList(1, lines.size() - 1);
    }

    private List<String> lettersOf(List<JsonNode> rows) {
        return rows.stream().map(row -> letterOf(row.get("talkId").asLong())).collect(Collectors.toList());
    }

    private List<String> lettersOfCsv(List<String> lines) {
        return lines.stream().map(line -> letterOf(Long.parseLong(line.split(",")[4]))).collect(Collectors.toList());
    }

    private static List<String> speakersOf(JsonNode row) {
        return StreamSupport.stream(row.get("speakers").spliterator(), false).map(JsonNode::asText)
                .collect(Collectors.toList());
    }

    private String letterOf(long talkId) {
        return talks.entrySet().stream().filter(talk -> talk.getValue() == talkId).map(Map.Entry::getKey)
                .findFirst().orElse("talk " + talkId);
    }

    /** Builds a JSON object body with Jackson, so titles with quotes and line breaks are escaped. */
    private static final class ObjectNodeBuilder {

        private final ObjectNode node = JSON.createObjectNode();

        ObjectNodeBuilder put(String field, String value) {
            node.put(field, value);
            return this;
        }

        ObjectNodeBuilder put(String field, long value) {
            node.put(field, value);
            return this;
        }

        @Override
        public String toString() {
            return node.toString();
        }
    }

    private static long id(HttpResponse<String> res) {
        assertEquals(201, res.statusCode(), res.body());
        try {
            return JSON.readTree(res.body()).get("id").asLong();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void expect(int status, HttpResponse<String> res) {
        assertEquals(status, res.statusCode(), res.body());
    }

    private HttpResponse<byte[]> get(String path, String accept) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (!accept.isEmpty()) {
            request.header("Accept", accept);
        }
        return http.sendAsync(request.build(), HttpResponse.BodyHandlers.ofByteArray()).join();
    }

    private HttpResponse<String> send(String method, String path, String json) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
    }
}
