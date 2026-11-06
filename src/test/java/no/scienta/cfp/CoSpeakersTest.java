package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

/**
 * Co-speakers on a talk: who can be one, how many, and where they show. Runs the real app on a random port with a
 * throwaway database.
 */
@DisplayName("Co-speakers")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CoSpeakersTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String UNREGISTERED_SPEAKER = "2147483647";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();

    /** Speaker names in the tables, and the ids they were registered with. */
    private final Map<String, String> speakers = new HashMap<>();

    private String talk;

    @BeforeEach
    void registerSpeakersAndSubmitATalk() throws Exception {
        for (String name : List.of("Main", "Ada", "Bob", "Cy")) {
            speakers.put(name, newSpeaker(name));
        }
        speakers.put("Nobody", UNREGISTERED_SPEAKER);
        talk = submit(speakers.get("Main"));
    }

    @DisplayName("R1, R2, R3, R5: Adds another registered speaker, at most two")
    @Description("""
            Main is the talk's main speaker, Ada, Bob and Cy are other registered speakers, and Nobody
            is a speaker id that is not registered. The co-speakers after are read from GET /talks/{id}.
            A blank message is not checked.
            """)
    @TableTest("""
        Scenario                        | Co-speakers before | Added                | Status? | Message?                                 | Co-speakers after?
        R1: first co-speaker            | []                 | Ada                  | 200     |                                          | [Ada]
        R1: second, in the order added  | [Ada]              | Bob                  | 200     |                                          | [Ada, Bob]
        R2: a third co-speaker          | [Ada, Bob]         | Cy                   | 409     | a talk has at most 2 co-speakers         | [Ada, Bob]
        R5: already a co-speaker        | [Ada]              | Ada                  | 200     |                                          | [Ada]
        R5: already one, at the limit   | [Ada, Bob]         | Bob                  | 200     |                                          | [Ada, Bob]
        R3: the main speaker            | []                 | Main                 | 409     | "the main speaker can't be a co-speaker" | []
        R1: an unregistered speaker     | []                 | Nobody               | 404     | speaker not found                        | []
        No speaker has id 0 or below    | []                 | {0, -1}              | 404     | speaker not found                        | []
        Speaker id that is not a number | []                 | abc                  | 400     |                                          | []
        Speaker id past 64 bits         | []                 | 99999999999999999999 | 400     |                                          | []
        """)
    void addsACoSpeaker(List<String> before, String added, int status, String message, List<String> after)
            throws Exception {
        add(before);
        assertAnswer(status, message, send("PUT", coSpeakerPath(added), null));
        assertEquals(after, coSpeakersOfTheTalk());
    }

    @DisplayName("R8: Removes a co-speaker, never the main speaker")
    @Description("""
            Removing a registered speaker who is not a co-speaker changes nothing, so a double-click
            on remove does not fail. A blank message is not checked.
            """)
    @TableTest("""
        Scenario                        | Co-speakers before | Removed              | Status? | Message?                            | Co-speakers after?
        A co-speaker                    | [Ada, Bob]         | Ada                  | 200     |                                     | [Bob]
        Not a co-speaker                | [Ada]              | Bob                  | 200     |                                     | [Ada]
        The main speaker                | [Ada]              | Main                 | 409     | "the main speaker can't be removed" | [Ada]
        An unregistered speaker         | [Ada]              | Nobody               | 404     | speaker not found                   | [Ada]
        No speaker has id 0 or below    | [Ada]              | {0, -1}              | 404     | speaker not found                   | [Ada]
        Speaker id that is not a number | [Ada]              | abc                  | 400     |                                     | [Ada]
        Speaker id past 64 bits         | [Ada]              | 99999999999999999999 | 400     |                                     | [Ada]
        """)
    void removesACoSpeaker(List<String> before, String removed, int status, String message, List<String> after)
            throws Exception {
        add(before);
        assertAnswer(status, message, send("DELETE", coSpeakerPath(removed), null));
        assertEquals(after, coSpeakersOfTheTalk());
    }

    @DisplayName("Answers 404 for an unknown talk and 400 for a talk id that is not a number")
    @TableTest("""
        Scenario                     | Method        | Talk                 | Status?
        Unknown talk                 | {PUT, DELETE} | 2147483647           | 404
        No talk has id 0 or below    | {PUT, DELETE} | {0, -1}              | 404
        Talk id that is not a number | {PUT, DELETE} | abc                  | 400
        Talk id past 64 bits         | {PUT, DELETE} | 99999999999999999999 | 400
        """)
    void answersForAnUnknownTalk(String method, String talkId, int status) throws Exception {
        HttpResponse<String> res = send(method, "/talks/" + talkId + "/co-speakers/" + speakers.get("Ada"), null);
        assertEquals(status, res.statusCode(), res.body());
    }

    @DisplayName("R4: Shows the co-speakers last, in the order added, wherever a talk is shown")
    @Description("""
            Bob and then Ada are added to Main's talk; Main's second talk has no co-speakers.
            :talk is the first talk's id and :main is Main's speaker id.
            """)
    @TableTest("""
        Scenario                      | Path                                | Co-speakers? | Without co-speakers?
        One talk                      | /talks/:talk                        | [Bob, Ada]   | []
        All talks                     | /talks                              | [Bob, Ada]   | []
        One speaker's talks           | /talks?speakerId=:main              | [Bob, Ada]   | []
        The ranking                   | /talks?sort=rating                  | [Bob, Ada]   | []
        One speaker's ranking         | /talks?speakerId=:main&sort=rating  | [Bob, Ada]   | []
        The speaker's own talk list   | /speakers/:main/talks               | [Bob, Ada]   | []
        Without withdrawn talks       | /talks?includeWithdrawn=false       | [Bob, Ada]   | []
        """)
    void showsTheCoSpeakersWhereverATalkIsShown(String path, List<String> coSpeakers, List<String> none)
            throws Exception {
        add(List.of("Bob", "Ada"));
        String other = submit(speakers.get("Main"));
        HttpResponse<String> res = send("GET", path.replace(":talk", talk).replace(":main", speakers.get("Main")), null);
        assertEquals(200, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        List<JsonNode> talks = body.isArray() ? StreamSupport.stream(body.spliterator(), false).toList() : List.of(body);
        talks.forEach(shown -> assertEquals("coSpeakerIds", lastFieldOf(shown), shown.toString()));
        assertEquals(coSpeakers, coSpeakersOf(talkWithId(talks, talk)));
        talks.stream().filter(shown -> shown.get("id").asText().equals(other))
                .forEach(shown -> assertEquals(none, coSpeakersOf(shown)));
    }

    @DisplayName("R4: Shows the co-speakers in the answers of the acceptance endpoints")
    @Description("""
            Ada is added to Main's talk, which the committee then waitlists, or Main withdraws.
            :talk is the talk's id and :main is Main's speaker id.
            """)
    @TableTest("""
        Scenario           | Method | Path                  | Body                      | Co-speakers?
        Committee decision | PUT    | /talks/:talk/status   | '{"status":"WAITLISTED"}' | [Ada]
        Main withdraws     | POST   | /talks/:talk/withdraw | '{"speakerId"::main}'     | [Ada]
        """)
    void showsTheCoSpeakersInTheAcceptanceAnswers(String method, String path, String body, List<String> coSpeakers)
            throws Exception {
        add(List.of("Ada"));
        HttpResponse<String> res = send(method, path.replace(":talk", talk),
                body.replace(":main", speakers.get("Main")));
        assertEquals(200, res.statusCode(), res.body());
        JsonNode shown = JSON.readTree(res.body());
        assertEquals(coSpeakers, coSpeakersOf(shown));
        assertEquals("coSpeakerIds", lastFieldOf(shown));
    }

    @Test
    @DisplayName("R4: A new talk shows no co-speakers")
    void aNewTalkShowsNoCoSpeakers() throws Exception {
        HttpResponse<String> res = send("POST", "/talks", talkBody(speakers.get("Main")));
        assertEquals(201, res.statusCode(), res.body());
        JsonNode shown = JSON.readTree(res.body());
        assertEquals(List.of(), coSpeakersOf(shown));
        assertEquals("coSpeakerIds", lastFieldOf(shown));
    }

    @Test
    @DisplayName("R4: A co-speaker removed and added again goes last")
    void aCoSpeakerAddedAgainGoesLast() throws Exception {
        add(List.of("Ada", "Bob"));
        assertEquals(200, send("DELETE", coSpeakerPath("Ada"), null).statusCode());
        HttpResponse<String> res = send("PUT", coSpeakerPath("Ada"), null);
        assertEquals(200, res.statusCode(), res.body());
        assertEquals(List.of("Bob", "Ada"), coSpeakersOf(JSON.readTree(res.body())));
    }

    @DisplayName("R6: A co-speaker's own talk list shows only their own talks")
    @Description("""
            Ada co-speaks on Main's talk and has submitted no talk herself. :ada is Ada's speaker id.
            """)
    @TableTest("""
        Scenario                | Path                  | Talks?
        The speaker's talk list | /speakers/:ada/talks  | 0
        The talks filter        | /talks?speakerId=:ada | 0
        """)
    void aCoSpeakersOwnTalkListShowsOnlyTheirOwnTalks(String path, int talks) throws Exception {
        add(List.of("Ada"));
        HttpResponse<String> res = send("GET", path.replace(":ada", speakers.get("Ada")), null);
        assertEquals(200, res.statusCode(), res.body());
        assertEquals(talks, JSON.readTree(res.body()).size(), res.body());
    }

    @Test
    @DisplayName("R7: A speaker with 3 talks of their own can still co-speak")
    void aSpeakerAtTheLimitCanStillCoSpeak() throws Exception {
        for (int i = 0; i < 3; i++) {
            submit(speakers.get("Ada"));
        }
        assertEquals(200, send("PUT", coSpeakerPath("Ada"), null).statusCode());
    }

    @Test
    @DisplayName("R7: Co-speaking does not count towards the limit of 3")
    void coSpeakingDoesNotCountTowardsTheLimit() throws Exception {
        add(List.of("Ada"));
        for (int i = 0; i < 3; i++) {
            assertEquals(201, send("POST", "/talks", talkBody(speakers.get("Ada"))).statusCode());
        }
        assertEquals(409, send("POST", "/talks", talkBody(speakers.get("Ada"))).statusCode());
    }

    @DisplayName("R9: A withdrawn or rejected talk can't be changed")
    @Description("""
            The talk has Ada as co-speaker. Main then withdraws it with POST /talks/{id}/withdraw, or
            the committee decides with PUT /talks/{id}/status, after two ratings for ACCEPTED.
            PUT adds Bob, DELETE removes Ada. A blank message is not checked.
            """)
    @TableTest("""
        Scenario       | Talk status                       | Request               | Status? | Message?
        Out for good   | {WITHDRAWN, REJECTED}             | {PUT Bob, DELETE Ada} | 409     | "a withdrawn or rejected talk can't be changed"
        Still on offer | {SUBMITTED, WAITLISTED, ACCEPTED} | {PUT Bob, DELETE Ada} | 200     |
        """)
    void aWithdrawnOrRejectedTalkCantBeChanged(String talkStatus, String request, int status, String message)
            throws Exception {
        add(List.of("Ada"));
        moveTheTalkTo(talkStatus);
        String[] methodAndSpeaker = request.split(" ");
        assertAnswer(status, message, send(methodAndSpeaker[0], coSpeakerPath(methodAndSpeaker[1]), null));
    }

    /** Moves the talk to the status through the acceptance endpoints. */
    private void moveTheTalkTo(String status) throws Exception {
        if (status.equals("ACCEPTED")) {
            for (int reviewer = 1; reviewer <= 2; reviewer++) {
                assertEquals(200, send("PUT", "/talks/" + talk + "/ratings/" + reviewer, "{\"score\":4}").statusCode());
            }
        }
        HttpResponse<String> res = switch (status) {
            case "SUBMITTED" -> send("GET", "/talks/" + talk, null);
            case "WITHDRAWN" -> send("POST", "/talks/" + talk + "/withdraw",
                    "{\"speakerId\":" + speakers.get("Main") + "}");
            default -> send("PUT", "/talks/" + talk + "/status", "{\"status\":\"" + status + "\"}");
        };
        assertEquals(status, JSON.readTree(res.body()).get("status").asText(), res.body());
    }

    @Test
    @DisplayName("R10: Simultaneous adds never give a talk more than two co-speakers")
    void simultaneousAddsNeverExceedTwoCoSpeakers() throws Exception {
        List<String> candidates = IntStream.range(0, 10).mapToObj(i -> uncheckedNewSpeaker("Candidate " + i)).toList();
        List<CompletableFuture<HttpResponse<String>>> sent = candidates.stream()
                .map(id -> http.sendAsync(request("PUT", "/talks/" + talk + "/co-speakers/" + id, null),
                        HttpResponse.BodyHandlers.ofString()))
                .toList();
        List<Integer> statuses = sent.stream().map(CompletableFuture::join).map(HttpResponse::statusCode).toList();
        assertEquals(2, statuses.stream().filter(s -> s == 200).count(), statuses.toString());
        assertEquals(8, statuses.stream().filter(s -> s == 409).count(), statuses.toString());
        assertEquals(2, JSON.readTree(send("GET", "/talks/" + talk, null).body()).get("coSpeakerIds").size());
    }

    @Test
    @DisplayName("R10: A double-click adds a co-speaker once")
    void aDoubleClickAddsACoSpeakerOnce() throws Exception {
        List<CompletableFuture<HttpResponse<String>>> sent = IntStream.range(0, 10)
                .mapToObj(i -> http.sendAsync(request("PUT", coSpeakerPath("Ada"), null),
                        HttpResponse.BodyHandlers.ofString()))
                .toList();
        sent.stream().map(CompletableFuture::join).forEach(res -> assertEquals(200, res.statusCode(), res.body()));
        assertEquals(List.of("Ada"), coSpeakersOfTheTalk());
    }

    @DisplayName("R12: Only the main speaker changes the co-speakers, when the request says who acts")
    @Description("""
            The talk has Ada as co-speaker. PUT adds Bob, DELETE removes Ada. Acting is the
            actingSpeakerId query parameter: a blank cell sends none, '' sends an empty value.
            A blank message is not checked.
            """)
    @TableTest("""
        Scenario                         | Request               | Acting               | Status? | Message?                                           | Co-speakers after?
        No acting speaker, as before     | PUT Bob               |                      | 200     |                                                    | [Ada, Bob]
        No acting speaker, as before     | DELETE Ada            |                      | 200     |                                                    | []
        An empty value, as none          | PUT Bob               | ''                   | 200     |                                                    | [Ada, Bob]
        An empty value, as none          | DELETE Ada            | ''                   | 200     |                                                    | []
        The main speaker                 | PUT Bob               | Main                 | 200     |                                                    | [Ada, Bob]
        The main speaker                 | DELETE Ada            | Main                 | 200     |                                                    | []
        A co-speaker                     | {PUT Bob, DELETE Ada} | Ada                  | 403     | only the main speaker can change the co-speakers   | [Ada]
        Another registered speaker       | {PUT Bob, DELETE Ada} | Cy                   | 403     | only the main speaker can change the co-speakers   | [Ada]
        An unregistered speaker          | {PUT Bob, DELETE Ada} | Nobody               | 403     | only the main speaker can change the co-speakers   | [Ada]
        No speaker has id 0 or below     | {PUT Bob, DELETE Ada} | {0, -1}              | 403     | only the main speaker can change the co-speakers   | [Ada]
        Acting id that is not a number   | {PUT Bob, DELETE Ada} | {abc, 1.5}           | 400     |                                                    | [Ada]
        Acting id past 64 bits           | {PUT Bob, DELETE Ada} | 99999999999999999999 | 400     |                                                    | [Ada]
        """)
    void onlyTheMainSpeakerChangesTheCoSpeakers(String request, String acting, int status, String message,
                                                List<String> after) throws Exception {
        add(List.of("Ada"));
        String[] methodAndSpeaker = request.split(" ");
        assertAnswer(status, message,
                send(methodAndSpeaker[0], coSpeakerPath(methodAndSpeaker[1]) + actingQuery(acting), null));
        assertEquals(after, coSpeakersOfTheTalk());
    }

    @DisplayName("R12: Who acts is checked after the talk is found and before the other rules")
    @Description("""
            The talk has Ada as co-speaker and the talk status given, or is a talk id that does not
            exist. Acting is the actingSpeakerId query parameter.
            """)
    @TableTest("""
        Scenario                           | Talk                  | Request                     | Acting | Status? | Message?
        Unknown talk first                 | unknown               | {PUT Bob, DELETE Ada}       | Cy     | 404     | talk not found
        Before an unregistered speaker     | SUBMITTED             | {PUT Nobody, DELETE Nobody} | Cy     | 403     | only the main speaker can change the co-speakers
        Before the main speaker as target  | SUBMITTED             | {PUT Main, DELETE Main}     | Cy     | 403     | only the main speaker can change the co-speakers
        Before the status                  | {WITHDRAWN, REJECTED} | {PUT Bob, DELETE Ada}       | Cy     | 403     | only the main speaker can change the co-speakers
        The main speaker gets the status   | {WITHDRAWN, REJECTED} | {PUT Bob, DELETE Ada}       | Main   | 409     | "a withdrawn or rejected talk can't be changed"
        """)
    void whoActsIsCheckedAfterTheTalkIsFound(String talkStatus, String request, String acting, int status,
                                             String message) throws Exception {
        add(List.of("Ada"));
        String[] methodAndSpeaker = request.split(" ");
        String path = coSpeakerPath(methodAndSpeaker[1]) + actingQuery(acting);
        if (talkStatus.equals("unknown")) {
            path = path.replace("/talks/" + talk + "/", "/talks/2147483647/");
        } else {
            moveTheTalkTo(talkStatus);
        }
        assertAnswer(status, message, send(methodAndSpeaker[0], path, null));
    }

    /** The actingSpeakerId query for a speaker in the tables or a literal value, none for null. */
    private String actingQuery(String acting) {
        return acting == null ? "" : "?actingSpeakerId=" + speakers.getOrDefault(acting, acting);
    }

    @Test
    @DisplayName("R11: A deleted speaker is no longer a co-speaker and leaves room for another")
    void aDeletedSpeakerIsNoLongerACoSpeaker() throws Exception {
        add(List.of("Ada", "Bob"));
        jdbc.update("DELETE FROM speaker WHERE id = ?", Long.parseLong(speakers.get("Bob")));
        assertEquals(List.of("Ada"), coSpeakersOfTheTalk());
        assertEquals(200, send("PUT", coSpeakerPath("Cy"), null).statusCode());
        assertEquals(List.of("Ada", "Cy"), coSpeakersOfTheTalk());
    }

    @Test
    @DisplayName("R11: A deleted talk leaves no co-speakers behind")
    void aDeletedTalkLeavesNoCoSpeakersBehind() throws Exception {
        add(List.of("Ada"));
        jdbc.update("DELETE FROM talk WHERE id = ?", Long.parseLong(talk));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM talk_co_speaker WHERE talk_id = ?", Integer.class,
                Long.parseLong(talk)));
    }

    /** The status, and the error message when the row gives one. */
    private static void assertAnswer(int status, String message, HttpResponse<String> res) throws Exception {
        assertEquals(status, res.statusCode(), res.body());
        if (message != null) {
            assertEquals(message, JSON.readTree(res.body()).get("message").asText(), res.body());
        }
    }

    private void add(List<String> coSpeakers) throws Exception {
        for (String name : coSpeakers) {
            HttpResponse<String> res = send("PUT", coSpeakerPath(name), null);
            assertEquals(200, res.statusCode(), res.body());
        }
    }

    /** The co-speaker path of the talk for a speaker in the tables, or for a literal id. */
    private String coSpeakerPath(String speaker) {
        return "/talks/" + talk + "/co-speakers/" + speakers.getOrDefault(speaker, speaker);
    }

    private List<String> coSpeakersOfTheTalk() throws Exception {
        return coSpeakersOf(JSON.readTree(send("GET", "/talks/" + talk, null).body()));
    }

    /** The talk's co-speakers by their names in the tables. */
    private List<String> coSpeakersOf(JsonNode shown) {
        Map<String, String> names = new HashMap<>();
        speakers.forEach((name, id) -> names.put(id, name));
        return StreamSupport.stream(shown.get("coSpeakerIds").spliterator(), false)
                .map(id -> names.get(id.asText()))
                .toList();
    }

    private static JsonNode talkWithId(List<JsonNode> talks, String id) {
        return talks.stream().filter(shown -> shown.get("id").asText().equals(id)).findFirst().orElseThrow();
    }

    private static String lastFieldOf(JsonNode shown) {
        List<String> names = StreamSupport.stream(((Iterable<String>) shown::fieldNames).spliterator(), false).toList();
        return names.get(names.size() - 1);
    }

    private String submit(String speakerId) throws Exception {
        return created(send("POST", "/talks", talkBody(speakerId)));
    }

    private static String talkBody(String speakerId) {
        return "{\"title\":\"Pair presenting\",\"format\":\"PRESENTATION\",\"level\":\"BEGINNER\",\"speakerId\":"
                + speakerId + "}";
    }

    private String newSpeaker(String name) throws Exception {
        return created(send("POST", "/speakers",
                "{\"name\":\"" + name + "\",\"email\":\"" + UUID.randomUUID() + "@example.com\"}"));
    }

    private String uncheckedNewSpeaker(String name) {
        try {
            return newSpeaker(name);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String created(HttpResponse<String> res) throws Exception {
        assertEquals(201, res.statusCode(), res.body());
        JsonNode id = JSON.readTree(res.body()).get("id");
        assertTrue(id != null && id.isNumber(), res.body());
        return id.asText();
    }

    private HttpResponse<String> send(String method, String path, String json) throws Exception {
        return http.send(request(method, path, json), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest request(String method, String path, String json) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                .build();
    }
}
