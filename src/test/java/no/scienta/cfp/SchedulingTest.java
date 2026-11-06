package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Programme scheduling: the fixed venue, putting accepted talks in a room and slot, and the rules that the programme
 * keeps (scheduling R1-R8). Runs the real app on a random port with a throwaway database, emptied before each row.
 * A talk in a table is a letter; the fixture creates it the first time a row names it.
 */
@DisplayName("Programme scheduling")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SchedulingTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> ROOMS = List.of("R1", "R2", "R3");
    private static final List<String> SLOTS = List.of("S1", "S2", "S3", "S4", "S5");

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
    private final Map<String, Long> talks = new LinkedHashMap<>();

    /** The speaker names of a row, and the IDs the fixture registered them with. */
    private final Map<String, Long> speakers = new HashMap<>();

    @BeforeEach
    void startWithAnEmptyProgramme() {
        jdbc.execute("TRUNCATE speaker, talk, rating, talk_co_speaker, programme_entry RESTART IDENTITY");
    }

    @DisplayName("R1: Gives each slot its fixed start and end")
    @Description("""
            Each row puts an accepted presentation in the room and slot, then looks the place up. The
            start and end are read from both answers. The same five slots exist in every room.
            """)
    @TableTest("""
        Scenario | Room | Slot | Start? | End?
        Slot S1  | R1   | S1   | 09:00  | 10:00
        Slot S2  | R2   | S2   | 10:15  | 11:15
        Slot S3  | R3   | S3   | 11:30  | 12:30
        Slot S4  | R1   | S4   | 13:30  | 15:30
        Slot S5  | R2   | S5   | 15:45  | 16:45
        """)
    void givesEachSlotItsFixedStartAndEnd(String room, String slot, String start, String end) throws Exception {
        String place = room + "/" + slot;
        Times expected = new Times(room, slot, start, end);
        assertEquals(expected, Times.of(send(Request.put("A", place))));
        assertEquals(expected, Times.of(send(Request.get(place))));
    }

    @DisplayName("R1: Refuses a room or slot outside the venue")
    @Description("""
            A PUT sends the ID of an accepted presentation. Rooms and slots are written in capitals.
            """)
    @TableTest("""
        Scenario           | Method             | Place | Status?
        Room R4            | {PUT, GET, DELETE} | R4/S1 | 400
        Slot S6            | {PUT, GET, DELETE} | R1/S6 | 400
        Room in lower case | {PUT, GET, DELETE} | r1/S1 | 400
        Slot in lower case | {PUT, GET, DELETE} | R1/s1 | 400
        """)
    void refusesARoomOrSlotOutsideTheVenue(String method, String place, int status) throws Exception {
        assertEquals(status, send(new Request(method, Optional.of("A"), place)).statusCode());
    }

    @DisplayName("R2: Puts a talk in a place, looks it up, takes it out")
    @Description("""
            A and B are accepted presentations of two different speakers. The programme before is set up
            with PUT. The requests of a row are sent one after the other, and the entry is the one in the
            answer to the last request. The programme after is read with GET on all 15 places.
            """)
    @TableTest("""
        Scenario                      | Programme Before | Requests                     | Statuses?  | Entry?     | Programme After?
        Empty place                   | [:]              | [PUT A R1/S1]                | [200]      | [R1/S1: A] | [R1/S1: A]
        Filled place looked up        | [R1/S1: A]       | [GET R1/S1]                  | [200]      | [R1/S1: A] | [R1/S1: A]
        Empty place looked up         | [R1/S1: A]       | [GET R1/S2]                  | [404]      |            | [R1/S1: A]
        Filled place emptied          | [R1/S1: A]       | [DELETE R1/S1, GET R1/S1]    | [204, 404] |            | [:]
        Empty place emptied           | [:]              | [DELETE R1/S1]               | [404]      |            | [:]
        Same place emptied twice      | [R1/S1: A]       | [DELETE R1/S1, DELETE R1/S1] | [204, 404] |            | [:]
        Taken out, put somewhere else | [R1/S1: A]       | [DELETE R1/S1, PUT A R2/S3]  | [204, 200] | [R2/S3: A] | [R2/S3: A]
        Same talk in same place again | [R1/S1: A]       | [PUT A R1/S1]                | [200]      | [R1/S1: A] | [R1/S1: A]
        """)
    void putsATalkInAPlaceLooksItUpTakesItOut(Map<String, String> programmeBefore, List<Request> requests,
                                              List<Integer> statuses, Map<String, String> entry,
                                              Map<String, String> programmeAfter) throws Exception {
        schedule(programmeBefore);
        List<HttpResponse<String>> answers = sendOneByOne(requests);
        assertEquals(statuses, statusesOf(answers));
        assertEquals(entry, entryOf(answers.get(answers.size() - 1)));
        assertEquals(programmeAfter, programme());
    }

    @DisplayName("R2: Puts a talk only for a body with a talk ID")
    @Description("""
            Each row creates talk A first, an accepted presentation, so A has ID 1. There is no talk
            999999999. Then it sends PUT /programme/R1/S1 with the body. A blank body sends no body.
            """)
    @TableTest("""
        Scenario                | Body                                | Status? | Programme After?
        Talk ID of A            | '{"talkId": 1}'                     | 200     | [R1/S1: A]
        Other fields as well    | '{"talkId": 1, "room": "R2"}'       | 200     | [R1/S1: A]
        No body                 |                                     | 400     | [:]
        Body that is not JSON   | '{"talkId":'                        | 400     | [:]
        Body that is a list     | '[1]'                               | 400     | [:]
        No talk ID              | '{}'                                | 400     | [:]
        Talk ID is null         | '{"talkId": null}'                  | 400     | [:]
        Talk ID is a string     | '{"talkId": "1"}'                   | 400     | [:]
        Talk ID with a fraction | '{"talkId": 1.0}'                   | 400     | [:]
        No such talk            | '{"talkId": 999999999}'             | 400     | [:]
        Talk ID 0 or below      | {'{"talkId": 0}', '{"talkId": -1}'} | 400     | [:]
        """)
    void putsATalkOnlyForABodyWithATalkId(String body, int status, Map<String, String> programmeAfter)
            throws Exception {
        talk("A");
        assertEquals(status, send("PUT", "/programme/R1/S1", body).join().statusCode());
        assertEquals(programmeAfter, programme());
    }

    @DisplayName("R3: Puts only an accepted talk on the programme")
    @Description("""
            Each row brings presentation A to the status through the acceptance API, then puts A in
            R1/S1. A withdrawn talk was accepted before its speaker withdrew it.
            """)
    @TableTest("""
        Scenario     | Talk Status                                  | Status? | Saved?
        Accepted     | ACCEPTED                                     | 200     | true
        Not accepted | {SUBMITTED, WAITLISTED, REJECTED, WITHDRAWN} | 409     | false
        """)
    void putsOnlyAnAcceptedTalkOnTheProgramme(String talkStatus, int status, boolean saved) throws Exception {
        talkWith("A", "PRESENTATION", talkStatus, Speakers.parse("Ada"));
        assertEquals(status, send(Request.put("A", "R1/S1")).statusCode());
        assertEquals(saved, programme().containsKey("R1/S1"));
    }

    @DisplayName("R4: Puts a workshop only in the long slot")
    @Description("""
            Each row puts accepted talk A of the format in room R1 and the slot. S4 is the long slot.
            """)
    @TableTest("""
        Scenario                  | Format                                     | Slot                 | Status? | Saved?
        Workshop in the long slot | WORKSHOP                                   | S4                   | 200     | true
        Workshop in a short slot  | WORKSHOP                                   | {S1, S2, S3, S5}     | 409     | false
        Other format in any slot  | {LIGHTNING_10, LIGHTNING_20, PRESENTATION} | {S1, S2, S3, S4, S5} | 200     | true
        """)
    void putsAWorkshopOnlyInTheLongSlot(String format, String slot, int status, boolean saved) throws Exception {
        talkWith("A", format, "ACCEPTED", Speakers.parse("Ada"));
        assertEquals(status, send(Request.put("A", "R1/" + slot)).statusCode());
        assertEquals(saved, programme().containsKey("R1/" + slot));
    }

    @DisplayName("R5: Keeps one talk per place, and each talk once")
    @Description("""
            A and B are accepted presentations of two different speakers. The programme before is set up
            with PUT. The requests of a row are sent one after the other, and the entry is the one in the
            answer to the last request.
            """)
    @TableTest("""
        Scenario                        | Programme Before | Requests                    | Statuses?  | Entry?     | Programme After?
        Place has another talk          | [R1/S1: B]       | [PUT A R1/S1]               | [409]      |            | [R1/S1: B]
        Talk in another room, same slot | [R1/S1: A]       | [PUT A R2/S1]               | [409]      |            | [R1/S1: A]
        Talk in another slot            | [R1/S1: A]       | [PUT A R1/S2]               | [409]      |            | [R1/S1: A]
        Talk taken out first            | [R1/S1: A]       | [DELETE R1/S1, PUT A R1/S2] | [204, 200] | [R1/S2: A] | [R1/S2: A]
        """)
    void keepsOneTalkPerPlaceAndEachTalkOnce(Map<String, String> programmeBefore, List<Request> requests,
                                             List<Integer> statuses, Map<String, String> entry,
                                             Map<String, String> programmeAfter) throws Exception {
        schedule(programmeBefore);
        List<HttpResponse<String>> answers = sendOneByOne(requests);
        assertEquals(statuses, statusesOf(answers));
        assertEquals(entry, entryOf(answers.get(answers.size() - 1)));
        assertEquals(programmeAfter, programme());
    }

    @DisplayName("R6: Keeps each speaker in one room at a time")
    @Description("""
            The speakers give each talk's main speaker first, then its co-speakers. Every talk is an
            accepted presentation. Ada, Bob and Cy are three registered speakers.
            """)
    @TableTest("""
        Scenario                         | Speakers                                                | Programme Before | Request     | Status? | Programme After?
        Same main speaker                | [A: Ada, B: Ada]                                        | [R1/S2: A]       | PUT B R2/S2 | 409     | [R1/S2: A]
        Co-speaker of the new talk       | [A: Ada, B: Bob with co-speaker Ada]                    | [R1/S2: A]       | PUT B R2/S2 | 409     | [R1/S2: A]
        Co-speaker of the scheduled talk | [A: Bob with co-speaker Ada, B: Ada]                    | [R1/S2: A]       | PUT B R2/S2 | 409     | [R1/S2: A]
        Co-speaker of both talks         | [A: Bob with co-speaker Ada, B: Cy with co-speaker Ada] | [R1/S2: A]       | PUT B R2/S2 | 409     | [R1/S2: A]
        Different speakers               | [A: Ada, B: Bob]                                        | [R1/S2: A]       | PUT B R2/S2 | 200     | [R1/S2: A, R2/S2: B]
        Same main speaker, another slot  | [A: Ada, B: Ada]                                        | [R1/S2: A]       | PUT B R2/S3 | 200     | [R1/S2: A, R2/S3: B]
        """)
    void keepsEachSpeakerInOneRoomAtATime(Map<String, Speakers> speakersOfTalks, Map<String, String> programmeBefore,
                                          Request request, int status, Map<String, String> programmeAfter)
            throws Exception {
        createTalks(speakersOfTalks);
        schedule(programmeBefore);
        assertEquals(status, send(request).statusCode());
        assertEquals(programmeAfter, programme());
    }

    @DisplayName("R7: Leaves the programme as it is when a talk changes")
    @Description("""
            Every talk is an accepted presentation. The change happens after the programme before is set
            up: the main speaker withdraws the talk, or a speaker becomes a co-speaker of the talk.
            """)
    @TableTest("""
        Scenario                   | Speakers         | Programme Before     | Change              | Request      | Status? | Programme After?
        Talk withdrawn, looked up  | [A: Ada]         | [R1/S1: A]           | A withdrawn         | GET R1/S1    | 200     | [R1/S1: A]
        Talk withdrawn, taken out  | [A: Ada]         | [R1/S1: A]           | A withdrawn         | DELETE R1/S1 | 204     | [:]
        Speaker in two rooms after | [A: Ada, B: Bob] | [R1/S2: A, R2/S2: B] | Ada co-speaker of B | GET R2/S2    | 200     | [R1/S2: A, R2/S2: B]
        """)
    void leavesTheProgrammeAsItIsWhenATalkChanges(Map<String, Speakers> speakersOfTalks,
                                                  Map<String, String> programmeBefore, Change change,
                                                  Request request, int status, Map<String, String> programmeAfter)
            throws Exception {
        createTalks(speakersOfTalks);
        schedule(programmeBefore);
        apply(change);
        assertEquals(status, send(request).statusCode());
        assertEquals(programmeAfter, programme());
    }

    @DisplayName("R8: Keeps the rules when requests come at the same time")
    @Description("""
            All requests of a row are sent at the same time to an empty programme. Every talk is an
            accepted presentation; a talk the speakers do not name has a speaker of its own. The service
            uses at most 5 database connections.
            """)
    @TableTest("""
        Scenario                         | Speakers         | Requests At Once                                                                                                                   | Succeeded? | Refused? | Entries After?
        Ten talks for one place          | [:]              | [PUT A R1/S1, PUT B R1/S1, PUT C R1/S1, PUT D R1/S1, PUT E R1/S1, PUT F R1/S1, PUT G R1/S1, PUT H R1/S1, PUT I R1/S1, PUT J R1/S1] | 1          | 9        | 1
        One talk for ten places          | [:]              | [PUT A R1/S1, PUT A R2/S1, PUT A R3/S1, PUT A R1/S2, PUT A R2/S2, PUT A R3/S2, PUT A R1/S3, PUT A R2/S3, PUT A R3/S3, PUT A R1/S5] | 1          | 9        | 1
        One speaker, two rooms, one slot | [A: Ada, B: Ada] | [PUT A R1/S2, PUT B R2/S2]                                                                                                         | 1          | 1        | 1
        """)
    void keepsTheRulesWhenRequestsComeAtTheSameTime(Map<String, Speakers> speakersOfTalks,
                                                    List<Request> requestsAtOnce, long succeeded, long refused,
                                                    int entriesAfter) throws Exception {
        createTalks(speakersOfTalks);
        requestsAtOnce.forEach(request -> request.talk().ifPresent(this::talk));
        Map<Integer, Long> statuses = sendAtOnce(requestsAtOnce).stream()
                .collect(Collectors.groupingBy(HttpResponse::statusCode, Collectors.counting()));
        assertEquals(succeeded, statuses.getOrDefault(200, 0L));
        assertEquals(refused, statuses.getOrDefault(409, 0L));
        assertEquals(entriesAfter, programme().size());
    }

    // ------------------------------------------------------------------------------------------- table types

    /** One request to the programme: {@code PUT A R1/S1}, {@code GET R1/S1} or {@code DELETE R1/S1}. */
    record Request(String method, Optional<String> talk, String place) {

        static Request put(String talk, String place) {
            return new Request("PUT", Optional.of(talk), place);
        }

        static Request get(String place) {
            return new Request("GET", Optional.empty(), place);
        }
    }

    @TypeConverter
    public static Request parseRequest(String cell) {
        String[] parts = cell.trim().split(" ");
        return parts.length == 3
                ? new Request(parts[0], Optional.of(parts[1]), parts[2])
                : new Request(parts[0], Optional.empty(), parts[1]);
    }

    /** The main speaker of a talk, then its co-speakers: {@code Bob with co-speaker Ada}. */
    record Speakers(String main, List<String> coSpeakers) {

        static Speakers parse(String cell) {
            String[] parts = cell.trim().split(" with co-speaker ");
            return new Speakers(parts[0], List.of(parts).subList(1, parts.length));
        }
    }

    @TypeConverter
    public static Speakers parseSpeakers(String cell) {
        return Speakers.parse(cell);
    }

    /** A change to a talk after it is on the programme: {@code A withdrawn} or {@code Ada co-speaker of B}. */
    record Change(String text) {
    }

    @TypeConverter
    public static Change parseChange(String cell) {
        return new Change(cell.trim());
    }

    /** The place and the times in an entry. */
    record Times(String room, String slot, String start, String end) {

        static Times of(HttpResponse<String> answer) throws Exception {
            JsonNode entry = JSON.readTree(answer.body());
            return new Times(entry.path("room").asText(null), entry.path("slot").asText(null),
                    entry.path("start").asText(null), entry.path("end").asText(null));
        }
    }

    // ------------------------------------------------------------------------------------------- fixture

    /** The ID of the talk, created as an accepted presentation of a speaker of its own if the row has not got it yet. */
    private long talk(String letter) {
        if (!talks.containsKey(letter)) {
            talkWith(letter, "PRESENTATION", "ACCEPTED", Speakers.parse("Speaker " + letter));
        }
        return talks.get(letter);
    }

    private void createTalks(Map<String, Speakers> speakersOfTalks) {
        speakersOfTalks.forEach((letter, talkSpeakers) ->
                talkWith(letter, "PRESENTATION", "ACCEPTED", talkSpeakers));
    }

    /** Submits the talk, adds its co-speakers, and brings it to the status through the acceptance API. */
    private void talkWith(String letter, String format, String status, Speakers talkSpeakers) {
        long main = speaker(talkSpeakers.main());
        long id = id(send("POST", "/talks", String.format(
                "{\"title\": \"Talk %s\", \"format\": \"%s\", \"level\": \"BEGINNER\", \"speakerId\": %d}",
                letter, format, main)).join());
        talkSpeakers.coSpeakers().forEach(co ->
                expect(200, send("PUT", "/talks/" + id + "/co-speakers/" + speaker(co), null).join()));
        talks.put(letter, id);
        bringTo(status, id, main);
    }

    private void bringTo(String status, long talk, long main) {
        switch (status) {
            case "SUBMITTED" -> { }
            case "WAITLISTED", "REJECTED" -> decide(talk, status);
            case "ACCEPTED" -> accept(talk);
            case "WITHDRAWN" -> {
                accept(talk);
                withdraw(talk, main);
            }
            default -> throw new IllegalArgumentException("no fixture for status " + status);
        }
    }

    private void accept(long talk) {
        IntStream.rangeClosed(1, 2).forEach(reviewer -> expect(200,
                send("PUT", "/talks/" + talk + "/ratings/" + reviewer, "{\"score\": 4}").join()));
        decide(talk, "ACCEPTED");
    }

    private void decide(long talk, String decision) {
        expect(200, send("PUT", "/talks/" + talk + "/status", "{\"status\": \"" + decision + "\"}").join());
    }

    private void withdraw(long talk, long main) {
        expect(200, send("POST", "/talks/" + talk + "/withdraw", "{\"speakerId\": " + main + "}").join());
    }

    private long speaker(String name) {
        return speakers.computeIfAbsent(name, newName -> id(send("POST", "/speakers", String.format(
                "{\"name\": \"%s\", \"email\": \"%s@example.com\"}",
                newName, newName.toLowerCase().replace(' ', '-'))).join()));
    }

    /** Applies a change from the R7 table. */
    private void apply(Change change) throws Exception {
        String[] words = change.text().split(" ");
        if (words.length == 2 && "withdrawn".equals(words[1])) {
            long talk = talks.get(words[0]);
            withdraw(talk, JSON.readTree(send("GET", "/talks/" + talk, null).join().body()).get("speakerId").asLong());
        } else if (words.length == 4 && "co-speaker".equals(words[1]) && "of".equals(words[2])) {
            expect(200, send("PUT", "/talks/" + talks.get(words[3]) + "/co-speakers/" + speaker(words[0]), null)
                    .join());
        } else {
            throw new IllegalArgumentException("no fixture for the change " + change.text());
        }
    }

    /** Puts each talk in its place, one after the other. */
    private void schedule(Map<String, String> programme) {
        programme.forEach((place, letter) -> expect(200, send(Request.put(letter, place))));
    }

    /** What is on the programme now, read with GET on every place: place to talk letter. */
    private Map<String, String> programme() throws Exception {
        Map<String, String> programme = new HashMap<>();
        for (String place : places()) {
            HttpResponse<String> answer = send(Request.get(place));
            if (answer.statusCode() == 200) {
                programme.put(place, letterOf(JSON.readTree(answer.body()).get("talkId").asLong()));
            }
        }
        return programme;
    }

    private static List<String> places() {
        return ROOMS.stream().flatMap(room -> SLOTS.stream().map(slot -> room + "/" + slot))
                .collect(Collectors.toList());
    }

    /** The entry in an answer as place to talk letter, or null when the answer has no entry. */
    private Map<String, String> entryOf(HttpResponse<String> answer) throws Exception {
        JsonNode body = answer.body().isEmpty() ? JSON.createObjectNode() : JSON.readTree(answer.body());
        if (!body.has("talkId")) {
            return null;
        }
        return Map.of(body.get("room").asText() + "/" + body.get("slot").asText(),
                letterOf(body.get("talkId").asLong()));
    }

    private String letterOf(long talkId) {
        return talks.entrySet().stream().filter(talk -> talk.getValue() == talkId).map(Map.Entry::getKey)
                .findFirst().orElse("talk " + talkId);
    }

    private static List<Integer> statusesOf(List<HttpResponse<String>> answers) {
        return answers.stream().map(HttpResponse::statusCode).collect(Collectors.toList());
    }

    private List<HttpResponse<String>> sendOneByOne(List<Request> requests) {
        return requests.stream().map(this::send).collect(Collectors.toList());
    }

    private List<HttpResponse<String>> sendAtOnce(List<Request> requests) {
        List<CompletableFuture<HttpResponse<String>>> sent = requests.stream().map(this::sendAsync)
                .collect(Collectors.toList());
        return sent.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    private HttpResponse<String> send(Request request) {
        return sendAsync(request).join();
    }

    private CompletableFuture<HttpResponse<String>> sendAsync(Request request) {
        String body = request.talk().filter(letter -> "PUT".equals(request.method()))
                .map(letter -> "{\"talkId\": " + talk(letter) + "}")
                .orElse(null);
        return send(request.method(), "/programme/" + request.place(), body);
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

    private CompletableFuture<HttpResponse<String>> send(String method, String path, String json) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
