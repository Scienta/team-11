package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
 * The change request submission-cap-cr: the limit of 3 talks counts the talks where a speaker is the main speaker or a
 * co-speaker, but not the talks that are rejected or withdrawn. Runs the real app on a random port with a throwaway
 * database. Each test registers new speakers, so the tests do not see each other's talks.
 */
@DisplayName("Submission cap (change request)")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubmissionCapTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();

    /** The speaker whose limit the tests check. */
    private long ada;

    @BeforeEach
    void registerAda() throws Exception {
        ada = newSpeaker("Ada");
    }

    @DisplayName("R1, R2, R4: Refuses a new talk from a speaker with 3 talks that count")
    @Description("""
            Ada first gets her own talks and her co-speaker places, each on a talk of another speaker,
            each in the status given. Then Ada submits one more talk of her own.
            """)
    @TableTest("""
        Scenario                              | Own Talks                         | Co-speaker Talks       | Limit | New Talk?
        Two of her own                        | [SUBMITTED, SUBMITTED]            | []                     | 3     | 201
        Three of her own, each in the running | [SUBMITTED, WAITLISTED, ACCEPTED] | []                     | 3     | 409
        Two of her own and one as co-speaker  | [SUBMITTED, SUBMITTED]            | [SUBMITTED]            | 3     | 409
        One of her own and two as co-speaker  | [SUBMITTED]                       | [WAITLISTED, ACCEPTED] | 3     | 409
        One of her own talks rejected         | [SUBMITTED, SUBMITTED, REJECTED]  | []                     | 3     | 201
        One of her own talks withdrawn        | [SUBMITTED, SUBMITTED, WITHDRAWN] | []                     | 3     | 201
        Her co-speaker talk rejected          | [SUBMITTED, SUBMITTED]            | [REJECTED]             | 3     | 201
        Her co-speaker talk withdrawn         | [SUBMITTED, SUBMITTED]            | [WITHDRAWN]            | 3     | 201
        """)
    void refusesANewTalkFromASpeakerWith3TalksThatCount(List<TalkStatus> ownTalks, List<TalkStatus> coSpeakerTalks,
                                                       int limit, int newTalk) throws Exception {
        giveAda(ownTalks, coSpeakerTalks);
        assertEquals(newTalk, postTalk(ada).statusCode());
    }

    @DisplayName("R1, R5: Refuses a co-speaker with 3 talks that count on a talk in the running")
    @Description("""
            Ada first gets her own talks and her co-speaker places, each in the status given. Then
            another speaker's talk gets the status in Target Talk, and Ada is added to it as co-speaker.
            A blank message is not checked. A target talk that is rejected or withdrawn keeps the
            answer of Co-speakers R9: the product owner confirmed it for this change request.
            """)
    @TableTest("""
        Scenario                             | Own Talks                         | Co-speaker Talks | Target Talk                       | Limit | Status? | Message?
        Two of her own                       | [SUBMITTED, SUBMITTED]            | []               | {SUBMITTED, WAITLISTED, ACCEPTED} | 3     | 200     |
        Three of her own                     | [SUBMITTED, SUBMITTED, SUBMITTED] | []               | {SUBMITTED, WAITLISTED, ACCEPTED} | 3     | 409     | this speaker already has 3 talks
        Two of her own and one as co-speaker | [SUBMITTED, SUBMITTED]            | [WAITLISTED]     | SUBMITTED                         | 3     | 409     | this speaker already has 3 talks
        One of her own talks rejected        | [SUBMITTED, SUBMITTED, REJECTED]  | []               | SUBMITTED                         | 3     | 200     |
        Her co-speaker talk withdrawn        | [SUBMITTED, SUBMITTED]            | [WITHDRAWN]      | SUBMITTED                         | 3     | 200     |
        Target talk out of the running       | [SUBMITTED, SUBMITTED, SUBMITTED] | []               | {REJECTED, WITHDRAWN}             | 3     | 409     | "a withdrawn or rejected talk can't be changed"
        """)
    void refusesACoSpeakerWith3TalksThatCount(List<TalkStatus> ownTalks, List<TalkStatus> coSpeakerTalks,
                                              TalkStatus targetTalk, int limit, int status, String message)
            throws Exception {
        giveAda(ownTalks, coSpeakerTalks);
        long target = talkOfAnotherSpeakerIn(targetTalk);
        assertAnswer(status, message, addAda(target));
    }

    @Test
    @DisplayName("R5: Adds a co-speaker at the limit again with no change")
    void addsACoSpeakerAtTheLimitAgainWithNoChange() throws Exception {
        long target = talkOfAnotherSpeakerIn(TalkStatus.SUBMITTED);
        giveAda(List.of(TalkStatus.SUBMITTED, TalkStatus.SUBMITTED), List.of());
        assertEquals(200, addAda(target).statusCode());

        assertEquals(200, addAda(target).statusCode());
    }

    @Test
    @DisplayName("R3: Frees the place at once when a co-speaker is taken off")
    void freesThePlaceWhenACoSpeakerIsTakenOff() throws Exception {
        giveAda(List.of(TalkStatus.SUBMITTED, TalkStatus.SUBMITTED), List.of());
        long target = talkOfAnotherSpeakerIn(TalkStatus.SUBMITTED);
        assertEquals(200, addAda(target).statusCode());

        assertEquals(200, send("DELETE", coSpeakerPath(target, ada), null).statusCode());

        assertEquals(201, postTalk(ada).statusCode());
    }

    @Test
    @DisplayName("R3: Frees the place at once when a talk is withdrawn")
    void freesThePlaceWhenATalkIsWithdrawn() throws Exception {
        long first = submitTalk(ada);
        giveAda(List.of(TalkStatus.SUBMITTED, TalkStatus.SUBMITTED), List.of());

        moveTo(first, ada, TalkStatus.WITHDRAWN);

        assertEquals(200, addAda(talkOfAnotherSpeakerIn(TalkStatus.SUBMITTED)).statusCode());
    }

    @Test
    @DisplayName("R6: Keeps the limit when submissions and co-speaker places arrive at the same time")
    void keepsTheLimitForRequestsAtTheSameTime() throws Exception {
        giveAda(List.of(TalkStatus.SUBMITTED, TalkStatus.SUBMITTED), List.of());
        List<Long> targets = IntStream.range(0, 5).mapToObj(n -> uncheckedTalkOfAnotherSpeaker()).toList();

        List<CompletableFuture<HttpResponse<String>>> sent = Stream.concat(
                        IntStream.range(0, 5).mapToObj(n -> sendAsync("POST", "/talks", talkBody(ada))),
                        targets.stream().map(target -> sendAsync("PUT", coSpeakerPath(target, ada), null)))
                .toList();
        Map<Integer, Long> statuses = sent.stream().map(CompletableFuture::join)
                .collect(Collectors.groupingBy(HttpResponse::statusCode, Collectors.counting()));

        assertEquals(9L, statuses.getOrDefault(409, 0L), statuses.toString());
        assertEquals(3, talksThatCountFor(ada));
    }

    @Test
    @DisplayName("R7: Keeps the talks of a speaker over the limit, and gives no new one")
    void keepsTheTalksOfASpeakerOverTheLimit() throws Exception {
        giveAda(List.of(TalkStatus.SUBMITTED, TalkStatus.SUBMITTED, TalkStatus.SUBMITTED), List.of());
        long fromBefore = talkOfAnotherSpeakerIn(TalkStatus.SUBMITTED);
        jdbc.update("INSERT INTO talk_co_speaker (talk_id, speaker_id) VALUES (?, ?)", fromBefore, ada);

        assertEquals(List.of(ada), coSpeakersOf(fromBefore));
        assertEquals(409, postTalk(ada).statusCode());
        assertEquals(409, addAda(talkOfAnotherSpeakerIn(TalkStatus.SUBMITTED)).statusCode());
    }

    // ------------------------------------------------------------------------------------------- fixture

    /** Gives Ada her own talks and her co-speaker places, each brought to its status after it is created. */
    private void giveAda(List<TalkStatus> ownTalks, List<TalkStatus> coSpeakerTalks) throws Exception {
        for (TalkStatus status : ownTalks) {
            moveTo(submitTalk(ada), ada, status);
        }
        for (TalkStatus status : coSpeakerTalks) {
            long host = newSpeaker("Host");
            long talk = submitTalk(host);
            assertEquals(200, addAda(talk).statusCode());
            moveTo(talk, host, status);
        }
    }

    private long talkOfAnotherSpeakerIn(TalkStatus status) throws Exception {
        long host = newSpeaker("Host");
        long talk = submitTalk(host);
        moveTo(talk, host, status);
        return talk;
    }

    private long uncheckedTalkOfAnotherSpeaker() {
        try {
            return talkOfAnotherSpeakerIn(TalkStatus.SUBMITTED);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Brings a new talk to the status through the acceptance API: two ratings before ACCEPTED. */
    private void moveTo(long talk, long mainSpeaker, TalkStatus status) throws Exception {
        switch (status) {
            case SUBMITTED -> { }
            case WITHDRAWN -> expect(200, send("POST", "/talks/" + talk + "/withdraw", "{\"speakerId\":" + mainSpeaker + "}"));
            case ACCEPTED -> {
                expect(200, send("PUT", "/talks/" + talk + "/ratings/1", "{\"score\":4}"));
                expect(200, send("PUT", "/talks/" + talk + "/ratings/2", "{\"score\":4}"));
                expect(200, send("PUT", "/talks/" + talk + "/status", "{\"status\":\"ACCEPTED\"}"));
            }
            case WAITLISTED, REJECTED -> expect(200, send("PUT", "/talks/" + talk + "/status",
                    "{\"status\":\"" + status.name() + "\"}"));
        }
    }

    private int talksThatCountFor(long speaker) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM talk t WHERE t.status IN ('SUBMITTED', 'WAITLISTED', 'ACCEPTED') "
                + "AND (t.speaker_id = ? OR EXISTS (SELECT 1 FROM talk_co_speaker c WHERE c.talk_id = t.id AND c.speaker_id = ?))",
                Integer.class, speaker, speaker);
        return count == null ? 0 : count;
    }

    private List<Long> coSpeakersOf(long talk) throws Exception {
        JsonNode shown = JSON.readTree(send("GET", "/talks/" + talk, null).body());
        return Stream.iterate(0, i -> i < shown.get("coSpeakerIds").size(), i -> i + 1)
                .map(i -> shown.get("coSpeakerIds").get(i).asLong())
                .toList();
    }

    private HttpResponse<String> addAda(long talk) throws Exception {
        return send("PUT", coSpeakerPath(talk, ada), null);
    }

    private static String coSpeakerPath(long talk, long speaker) {
        return "/talks/" + talk + "/co-speakers/" + speaker;
    }

    private long submitTalk(long speaker) throws Exception {
        HttpResponse<String> res = postTalk(speaker);
        expect(201, res);
        return JSON.readTree(res.body()).get("id").asLong();
    }

    private HttpResponse<String> postTalk(long speaker) throws Exception {
        return send("POST", "/talks", talkBody(speaker));
    }

    private static String talkBody(long speaker) {
        return "{\"title\":\"Pair presenting\",\"format\":\"PRESENTATION\",\"level\":\"BEGINNER\",\"speakerId\":"
                + speaker + "}";
    }

    private long newSpeaker(String name) throws Exception {
        HttpResponse<String> res = send("POST", "/speakers",
                "{\"name\":\"" + name + "\",\"email\":\"" + UUID.randomUUID() + "@example.com\"}");
        expect(201, res);
        return JSON.readTree(res.body()).get("id").asLong();
    }

    private static void assertAnswer(int status, String message, HttpResponse<String> res) throws Exception {
        assertEquals(status, res.statusCode(), res.body());
        if (message != null) {
            assertEquals(message, JSON.readTree(res.body()).get("message").asText(), res.body());
        }
    }

    private static void expect(int status, HttpResponse<String> res) {
        assertEquals(status, res.statusCode(), res.body());
    }

    private HttpResponse<String> send(String method, String path, String json) throws Exception {
        return http.send(request(method, path, json), HttpResponse.BodyHandlers.ofString());
    }

    private CompletableFuture<HttpResponse<String>> sendAsync(String method, String path, String json) {
        return http.sendAsync(request(method, path, json), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest request(String method, String path, String json) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                .build();
    }
}
