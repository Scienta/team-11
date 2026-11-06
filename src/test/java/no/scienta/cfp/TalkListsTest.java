package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

/**
 * The committee's talk lists from the ratings spec: all talks or one speaker's talks, oldest first (R5), and the
 * ranking by rating (R6). Runs the real app on a random port with a throwaway database, emptied before each test, so
 * that each row sees only its own talks.
 */
@DisplayName("Ratings: talk lists")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TalkListsTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Autowired
    JdbcTemplate jdbc;

    private final ApiClient api;

    TalkListsTest(@Value("${local.server.port}") int port) {
        api = new ApiClient(port);
    }

    @BeforeEach
    void startWithNoTalks() {
        jdbc.execute("TRUNCATE speaker, talk, rating, talk_co_speaker RESTART IDENTITY");
    }

    @DisplayName("R6: Ranks talks by shown average, then rating count, then age")
    @Description("""
            Talks A, B and C are submitted in this order, so A is the oldest. Each score comes from a
            different reviewer. An empty list means that nobody has rated the talk. The ranking is
            GET /talks?sort=rating.
            """)
    @TableTest("""
        Scenario                         | Talk A       | Talk B    | Talk C    | Ranking?
        Different averages               | [3]          | [5]       | [4]       | [B, C, A]
        Same average, different counts   | [4]          | [4, 4]    | [4, 4, 4] | [C, B, A]
        Same average only after rounding | [4, 4, 4, 5] | [4, 4, 5] | [5]       | [C, A, B]
        Same average and count           | [4]          | [4]       | [4]       | [A, B, C]
        Unrated talks                    | []           | [1]       | []        | [B, A, C]
        """)
    void ranksTalksByShownAverageThenCountThenAge(List<Integer> talkA, List<Integer> talkB, List<Integer> talkC,
                                                  List<String> ranking) throws Exception {
        long speaker = registerSpeaker("ada@example.com");
        rate(submitTalk("A", speaker), talkA);
        rate(submitTalk("B", speaker), talkB);
        rate(submitTalk("C", speaker), talkC);
        assertEquals(ranking, titles(api.get("/talks?sort=rating")));
    }

    @DisplayName("R5: Lists all talks oldest first, or the talks of one speaker")
    @Description("""
            Speaker 1 submits talk A, speaker 2 submits talk B, then speaker 1 submits talk C.
            A blank speaker sends GET /talks without a speakerId.
            """)
    @TableTest("""
        Scenario                | Speaker | Talks?
        All speakers            |         | [A, B, C]
        One speaker             | 1       | [A, C]
        Another speaker         | 2       | [B]
        Speaker that is unknown | 3       | []
        """)
    void listsTalksOldestFirst(Long speakerId, List<String> talks) throws Exception {
        long first = registerSpeaker("ada@example.com");
        long second = registerSpeaker("grace@example.com");
        submitTalk("A", first);
        submitTalk("B", second);
        submitTalk("C", first);
        assertEquals(talks, titles(api.get(talkListPath(speakerId))));
    }

    @DisplayName("Ranks talks only for the sort order rating")
    @Description("""
            A blank sort sends GET /talks without a sort, which lists the talks oldest first.
            """)
    @TableTest("""
        Scenario           | Sort          | Status?
        No sort            |               | 200
        Rating             | rating        | 200
        Another sort order | {date, title} | 400
        Rating in capitals | RATING        | 400
        """)
    void ranksTalksOnlyForSortOrderRating(String sort, int status) throws Exception {
        assertEquals(status, api.get(sortPath(sort)).statusCode());
    }

    private long registerSpeaker(String email) throws Exception {
        HttpResponse<String> res = api.send("POST", "/speakers",
                JSON.writeValueAsString(JSON.createObjectNode().put("name", "Grace Hopper").put("email", email)));
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body()).get("id").asLong();
    }

    private long submitTalk(String title, long speakerId) throws Exception {
        HttpResponse<String> res = api.send("POST", "/talks", JSON.writeValueAsString(JSON.createObjectNode()
                .put("title", title).put("format", "PRESENTATION").put("level", "BEGINNER").put("speakerId", speakerId)));
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body()).get("id").asLong();
    }

    /** Rates the talk once per score, each score from a different reviewer. */
    private void rate(long talkId, List<Integer> scores) throws Exception {
        for (int reviewer = 1; reviewer <= scores.size(); reviewer++) {
            HttpResponse<String> res = api.send("PUT", "/talks/" + talkId + "/ratings/" + reviewer,
                    "{\"score\":" + scores.get(reviewer - 1) + "}");
            assertEquals(200, res.statusCode(), res.body());
        }
    }

    private static List<String> titles(HttpResponse<String> res) throws Exception {
        assertEquals(200, res.statusCode(), res.body());
        JsonNode talks = JSON.readTree(res.body());
        return StreamSupport.stream(talks.spliterator(), false)
                .map(talk -> talk.get("title").asText())
                .collect(Collectors.toList());
    }

    private static String talkListPath(Long speakerId) {
        return speakerId == null ? "/talks" : "/talks?speakerId=" + speakerId;
    }

    private static String sortPath(String sort) {
        return sort == null ? "/talks" : "/talks?sort=" + sort;
    }
}
