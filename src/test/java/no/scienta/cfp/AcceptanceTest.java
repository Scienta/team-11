package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
import org.tabletest.junit.TypeConverter;

/**
 * The acceptance state machine (workflow/specs/acceptance/spec.md): the committee's decisions, the speaker's
 * withdrawal, and their effect on the talk limit, the ratings and the talk lists. Runs the real app on a random port
 * with a throwaway database, emptied before each test.
 */
@DisplayName("Acceptance")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AcceptanceTest {

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

    @BeforeEach
    void startWithNoTalks() {
        jdbc.execute("TRUNCATE speaker, talk, rating RESTART IDENTITY");
    }

    @DisplayName("R2: Moves a talk to the committee's decision")
    @Description("""
            The talk has ratings from two different reviewers before it gets its status before.
            A refused decision leaves the status as it was (R6).
            """)
    @TableTest("""
        Scenario                  | Status Before           | Decision                         | Response? | Status After?
        Open talk accepted        | {SUBMITTED, WAITLISTED} | ACCEPTED                         | 200       | ACCEPTED
        Open talk rejected        | {SUBMITTED, WAITLISTED} | REJECTED                         | 200       | REJECTED
        Submitted talk waitlisted | SUBMITTED               | WAITLISTED                       | 200       | WAITLISTED
        Same decision again       | WAITLISTED              | WAITLISTED                       | 409       | WAITLISTED
        Accepted talk             | ACCEPTED                | {ACCEPTED, REJECTED, WAITLISTED} | 409       | ACCEPTED
        Rejected talk             | REJECTED                | {ACCEPTED, REJECTED, WAITLISTED} | 409       | REJECTED
        Withdrawn talk            | WITHDRAWN               | {ACCEPTED, REJECTED, WAITLISTED} | 409       | WITHDRAWN
        """)
    void movesTalkToTheDecision(TalkStatus statusBefore, TalkStatus decision, int response, TalkStatus statusAfter)
            throws Exception {
        long talk = ratedTalkIn(statusBefore, registerSpeaker("ada@example.com"));
        assertEquals(response, decide(talk, decision).statusCode());
        assertEquals(statusAfter, statusOf(talk));
    }

    @DisplayName("R3: Accepts a talk only after two different reviewers")
    @Description("""
            Each number is one rating by that reviewer, in this order. Reviewer 1 twice is one reviewer
            whose second score replaces the first.
            """)
    @TableTest("""
        Scenario                     | Ratings From Reviewers | Decision   | Response? | Status After?
        No ratings                   | []                     | ACCEPTED   | 409       | SUBMITTED
        One reviewer                 | [1]                    | ACCEPTED   | 409       | SUBMITTED
        One reviewer who rated twice | [1, 1]                 | ACCEPTED   | 409       | SUBMITTED
        Two reviewers                | [1, 2]                 | ACCEPTED   | 200       | ACCEPTED
        No ratings, rejection        | []                     | REJECTED   | 200       | REJECTED
        No ratings, waitlist         | []                     | WAITLISTED | 200       | WAITLISTED
        """)
    void acceptsOnlyAfterTwoReviewers(List<Long> reviewers, TalkStatus decision, int response,
                                      TalkStatus statusAfter) throws Exception {
        long talk = submitTalk("A", registerSpeaker("ada@example.com"));
        for (long reviewer : reviewers) {
            rate(talk, reviewer);
        }
        assertEquals(response, decide(talk, decision).statusCode());
        assertEquals(statusAfter, statusOf(talk));
    }

    @DisplayName("R1: Takes only the three decisions as a status")
    @TableTest("""
        Scenario       | Body                                                                             | Response?
        A decision     | '{"status":"WAITLISTED"}'                                                        | 200
        Not a decision | {'{"status":"SUBMITTED"}', '{"status":"WITHDRAWN"}'}                             | 400
        Not a status   | {'{"status":"accepted"}', '{"status":"MAYBE"}', '{"status":""}', '{"status":1}'} | 400
        No status      | '{}'                                                                             | 400
        """)
    void takesOnlyTheThreeDecisions(String body, int response) throws Exception {
        long talk = submitTalk("A", registerSpeaker("ada@example.com"));
        assertEquals(response, send("PUT", "/talks/" + talk + "/status", body).statusCode());
    }

    @DisplayName("R4: Lets only the main speaker withdraw a talk")
    @Description("""
            Speaker 1 submits the talk. Speaker 2 is registered too. Speaker 99 does not exist.
            A blank speaker sends a body without speakerId.
            """)
    @TableTest("""
        Scenario         | Speaker Asking | Response? | Status After?
        The main speaker | 1              | 200       | WITHDRAWN
        Somebody else    | {2, 99}        | 403       | SUBMITTED
        No speaker       |                | 400       | SUBMITTED
        """)
    void letsOnlyTheMainSpeakerWithdraw(Long speakerAsking, int response, TalkStatus statusAfter) throws Exception {
        long talk = submitTalk("A", registerSpeaker("ada@example.com"));
        registerSpeaker("grace@example.com");
        assertEquals(response, withdraw(talk, speakerAsking).statusCode());
        assertEquals(statusAfter, statusOf(talk));
    }

    @DisplayName("R4: Withdraws a talk unless it is rejected or withdrawn")
    @Description("""
            The main speaker asks. The talk has ratings from two different reviewers before it gets its
            status before. A refused withdrawal leaves the status as it was (R6).
            """)
    @TableTest("""
        Scenario       | Status Before                     | Response? | Status After?
        Talk in play   | {SUBMITTED, WAITLISTED, ACCEPTED} | 200       | WITHDRAWN
        Rejected talk  | REJECTED                          | 409       | REJECTED
        Withdrawn talk | WITHDRAWN                         | 409       | WITHDRAWN
        """)
    void withdrawsUnlessRejectedOrWithdrawn(TalkStatus statusBefore, int response, TalkStatus statusAfter)
            throws Exception {
        long speaker = registerSpeaker("ada@example.com");
        long talk = ratedTalkIn(statusBefore, speaker);
        assertEquals(response, withdraw(talk, speaker).statusCode());
        assertEquals(statusAfter, statusOf(talk));
    }

    @DisplayName("R5: Shows the current status wherever the talk is shown")
    @Description("""
            One speaker has one talk, rated by two different reviewers before it gets its status. In a
            place, {talk} is the talk's id and {speaker} is the speaker's id. In a list, the test reads
            the status of this talk.
            """)
    @TableTest("""
        Scenario        | Status     | Place                                                                                                    | Status Shown?
        Submitted talk  | SUBMITTED  | {'/talks/{talk}', /talks, '/talks?speakerId={speaker}', /talks?sort=rating, '/speakers/{speaker}/talks'} | SUBMITTED
        Accepted talk   | ACCEPTED   | {'/talks/{talk}', /talks, '/talks?speakerId={speaker}', /talks?sort=rating, '/speakers/{speaker}/talks'} | ACCEPTED
        Rejected talk   | REJECTED   | {'/talks/{talk}', /talks, '/talks?speakerId={speaker}', /talks?sort=rating, '/speakers/{speaker}/talks'} | REJECTED
        Waitlisted talk | WAITLISTED | {'/talks/{talk}', /talks, '/talks?speakerId={speaker}', /talks?sort=rating, '/speakers/{speaker}/talks'} | WAITLISTED
        Withdrawn talk  | WITHDRAWN  | {'/talks/{talk}', /talks, '/talks?speakerId={speaker}', /talks?sort=rating, '/speakers/{speaker}/talks'} | WITHDRAWN
        """)
    void showsTheStatusWhereverTheTalkIsShown(TalkStatus status, String place, TalkStatus statusShown)
            throws Exception {
        long speaker = registerSpeaker("ada@example.com");
        long talk = ratedTalkIn(status, speaker);
        assertEquals(statusShown, statusShownAt(place, talk, speaker));
    }

    @DisplayName("Answers 404 for a talk that does not exist")
    @TableTest("""
        Scenario               | Request                                                                                   | Response?
        Decision or withdrawal | {'PUT /talks/99/status {"status":"REJECTED"}', 'POST /talks/99/withdraw {"speakerId":1}'} | 404
        """)
    void answersNotFoundForUnknownTalk(ApiRequest request, int response) throws Exception {
        registerSpeaker("ada@example.com");
        assertEquals(response, send(request.method(), request.path(), request.body()).statusCode());
    }

    @DisplayName("R6: Lets only one of two concurrent changes win")
    @Test
    void letsOnlyOneOfTwoConcurrentChangesWin() throws Exception {
        for (int round = 0; round < 10; round++) {
            long speaker = registerSpeaker("speaker" + round + "@example.com");
            long accepted = ratedTalkIn(TalkStatus.SUBMITTED, speaker);
            assertOneWins(accepted,
                    () -> decide(accepted, TalkStatus.ACCEPTED), TalkStatus.ACCEPTED,
                    () -> decide(accepted, TalkStatus.REJECTED), TalkStatus.REJECTED);
            long withdrawn = ratedTalkIn(TalkStatus.SUBMITTED, speaker);
            assertOneWins(withdrawn,
                    () -> decide(withdrawn, TalkStatus.REJECTED), TalkStatus.REJECTED,
                    () -> withdraw(withdrawn, speaker), TalkStatus.WITHDRAWN);
        }
    }

    @DisplayName("R7: Leaves a withdrawn talk out of the talk limit")
    @Description("""
            The speaker has three talks, and the third has the status in the table. Then the speaker
            submits another talk.
            """)
    @TableTest("""
        Scenario                | Third Talk                                  | Response?
        Third talk still counts | {SUBMITTED, ACCEPTED, REJECTED, WAITLISTED} | 409
        Third talk withdrawn    | WITHDRAWN                                   | 201
        """)
    void leavesWithdrawnTalkOutOfTheLimit(TalkStatus thirdTalk, int response) throws Exception {
        long speaker = registerSpeaker("ada@example.com");
        submitTalk("A", speaker);
        submitTalk("B", speaker);
        ratedTalkIn(thirdTalk, speaker);
        assertEquals(response, postTalk("D", speaker).statusCode());
    }

    @DisplayName("R8: Takes ratings only for a talk that is still open")
    @Description("""
            Reviewers 1 and 2 rate the talk before it gets its status. Then reviewer 3 rates it.
            """)
    @TableTest("""
        Scenario     | Status                          | Response? | Rating Count After?
        Open talk    | {SUBMITTED, WAITLISTED}         | 200       | 3
        Decided talk | {ACCEPTED, REJECTED, WITHDRAWN} | 409       | 2
        """)
    void takesRatingsOnlyForOpenTalk(TalkStatus status, int response, int ratingCountAfter) throws Exception {
        long talk = ratedTalkIn(status, registerSpeaker("ada@example.com"));
        assertEquals(response, rate(talk, 3L).statusCode());
        assertEquals(ratingCountAfter, talkJson(talk).get("ratingCount").asInt());
    }

    @DisplayName("R9: Shows withdrawn talks after the others")
    @Description("""
            Talks A, B and C are submitted in this order. Each score comes from a different reviewer. An
            empty list means that nobody has rated the talk. The speaker withdraws the talks in the
            Withdrawn column after the ratings.
            """)
    @TableTest("""
        Scenario                         | Talk A | Talk B | Talk C | Withdrawn | List                                      | Titles?
        Oldest first                     | [5]    | [3]    | [4]    | [A]       | {/talks, /talks?includeWithdrawn=true}    | [B, C, A]
        One speaker's talks              | [5]    | [3]    | [4]    | [A]       | '/talks?speakerId=1'                      | [B, C, A]
        Ranking, best talk withdrawn     | [5]    | [3]    | [4]    | [A]       | /talks?sort=rating                        | [C, B, A]
        Ranking with an unrated talk     | [5]    | []     | [4]    | [A]       | /talks?sort=rating                        | [C, B, A]
        Ranking, two talks withdrawn     | [5]    | [3]    | [4]    | [A, B]    | /talks?sort=rating                        | [C, A, B]
        Oldest first, withdrawn left out | [5]    | [3]    | [4]    | [A]       | /talks?includeWithdrawn=false             | [B, C]
        Ranking, withdrawn left out      | [5]    | [3]    | [4]    | [A]       | /talks?sort=rating&includeWithdrawn=false | [C, B]
        """)
    void showsWithdrawnTalksAfterTheOthers(List<Integer> talkA, List<Integer> talkB, List<Integer> talkC,
                                           List<String> withdrawn, String list, List<String> titles)
            throws Exception {
        long speaker = registerSpeaker("ada@example.com");
        Map<String, Long> talks = Map.of("A", submitTalk("A", speaker), "B", submitTalk("B", speaker),
                "C", submitTalk("C", speaker));
        rateWithScores(talks.get("A"), talkA);
        rateWithScores(talks.get("B"), talkB);
        rateWithScores(talks.get("C"), talkC);
        for (String title : withdrawn) {
            assertEquals(200, withdraw(talks.get(title), speaker).statusCode());
        }
        assertEquals(titles, titles(send("GET", list, null)));
    }

    @DisplayName("R9: Takes only true or false to leave out withdrawn talks")
    @Description("""
            An empty value, as in /talks?includeWithdrawn=, counts as no value and lists all talks.
            """)
    @TableTest("""
        Scenario      | Include Withdrawn | Response?
        True or false | {true, false}     | 200
        Empty value   | ''                | 200
        Another value | {yes, 1, TRUE}    | 400
        """)
    void takesOnlyTrueOrFalseForIncludeWithdrawn(String includeWithdrawn, int response) throws Exception {
        assertEquals(response, send("GET", "/talks?includeWithdrawn=" + includeWithdrawn, null).statusCode());
    }

    /** An HTTP request written as {@code METHOD /path body}. */
    record ApiRequest(String method, String path, String body) {
    }

    @TypeConverter
    public static ApiRequest parseApiRequest(String request) {
        String[] parts = request.split(" ", 3);
        return new ApiRequest(parts[0], parts[1], parts.length == 3 ? parts[2] : null);
    }

    private void assertOneWins(long talk, Callable<HttpResponse<String>> first, TalkStatus firstStatus,
                               Callable<HttpResponse<String>> second, TalkStatus secondStatus) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Future<HttpResponse<String>> one = pool.submit(() -> {
                start.await();
                return first.call();
            });
            Future<HttpResponse<String>> two = pool.submit(() -> {
                start.await();
                return second.call();
            });
            start.countDown();
            List<Integer> responses = List.of(one.get().statusCode(), two.get().statusCode());
            assertEquals(Set.of(200, 409), Set.copyOf(responses), "responses " + responses);
            TalkStatus winner = responses.get(0) == 200 ? firstStatus : secondStatus;
            assertEquals(winner, statusOf(talk));
        } finally {
            pool.shutdown();
        }
    }

    /** Submits a talk, has reviewers 1 and 2 rate it, and moves it to the status through the API. */
    private long ratedTalkIn(TalkStatus status, long speaker) throws Exception {
        long talk = submitTalk("T", speaker);
        rate(talk, 1L);
        rate(talk, 2L);
        switch (status) {
            case SUBMITTED:
                break;
            case WITHDRAWN:
                assertEquals(200, withdraw(talk, speaker).statusCode());
                break;
            case ACCEPTED:
            case REJECTED:
            case WAITLISTED:
                assertEquals(200, decide(talk, status).statusCode());
                break;
        }
        assertEquals(status, statusOf(talk));
        return talk;
    }

    private long registerSpeaker(String email) throws Exception {
        HttpResponse<String> res = send("POST", "/speakers",
                JSON.writeValueAsString(JSON.createObjectNode().put("name", "Grace Hopper").put("email", email)));
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body()).get("id").asLong();
    }

    private long submitTalk(String title, long speakerId) throws Exception {
        HttpResponse<String> res = postTalk(title, speakerId);
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body()).get("id").asLong();
    }

    private HttpResponse<String> postTalk(String title, long speakerId) throws Exception {
        return send("POST", "/talks", JSON.writeValueAsString(JSON.createObjectNode()
                .put("title", title).put("format", "PRESENTATION").put("level", "BEGINNER").put("speakerId", speakerId)));
    }

    private HttpResponse<String> rate(long talkId, long reviewer) throws Exception {
        return send("PUT", "/talks/" + talkId + "/ratings/" + reviewer, "{\"score\":4}");
    }

    /** Rates the talk once per score, each score from a different reviewer. */
    private void rateWithScores(long talkId, List<Integer> scores) throws Exception {
        for (int reviewer = 1; reviewer <= scores.size(); reviewer++) {
            HttpResponse<String> res = send("PUT", "/talks/" + talkId + "/ratings/" + reviewer,
                    "{\"score\":" + scores.get(reviewer - 1) + "}");
            assertEquals(200, res.statusCode(), res.body());
        }
    }

    private HttpResponse<String> decide(long talkId, TalkStatus decision) throws Exception {
        return send("PUT", "/talks/" + talkId + "/status", "{\"status\":\"" + decision.name() + "\"}");
    }

    private HttpResponse<String> withdraw(long talkId, Long speakerId) throws Exception {
        String body = speakerId == null ? "{}" : "{\"speakerId\":" + speakerId + "}";
        return send("POST", "/talks/" + talkId + "/withdraw", body);
    }

    /** The status of the talk as the place shows it: the talk itself, or the talk's entry in a list. */
    private TalkStatus statusShownAt(String place, long talkId, long speakerId) throws Exception {
        String path = place.replace("{talk}", Long.toString(talkId)).replace("{speaker}", Long.toString(speakerId));
        HttpResponse<String> res = send("GET", path, null);
        assertEquals(200, res.statusCode(), res.body());
        JsonNode shown = JSON.readTree(res.body());
        JsonNode entry = StreamSupport.stream(shown.isArray() ? shown.spliterator() : List.of(shown).spliterator(), false)
                .filter(talk -> talk.get("id").asLong() == talkId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("talk " + talkId + " not shown at " + path));
        return TalkStatus.valueOf(entry.get("status").asText());
    }

    private TalkStatus statusOf(long talkId) throws Exception {
        return TalkStatus.valueOf(talkJson(talkId).get("status").asText());
    }

    private JsonNode talkJson(long talkId) throws Exception {
        HttpResponse<String> res = send("GET", "/talks/" + talkId, null);
        assertEquals(200, res.statusCode(), res.body());
        return JSON.readTree(res.body());
    }

    private static List<String> titles(HttpResponse<String> res) throws Exception {
        assertEquals(200, res.statusCode(), res.body());
        JsonNode talks = JSON.readTree(res.body());
        return StreamSupport.stream(talks.spliterator(), false)
                .map(talk -> talk.get("title").asText())
                .collect(Collectors.toList());
    }

    private HttpResponse<String> send(String method, String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
