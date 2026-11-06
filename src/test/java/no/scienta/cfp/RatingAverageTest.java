package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
import org.tabletest.junit.TypeConverter;

/**
 * The ratings change request (workflow/specs/ratings-cr/spec.md): the trimmed average with two decimals, and the
 * ranking by it. Runs the real app on a random port with a throwaway database, emptied before each test.
 */
@DisplayName("Ratings change request")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RatingAverageTest {

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

    private long ada;
    private long grace;

    @BeforeEach
    void startWithTwoSpeakersAndNoTalks() throws Exception {
        jdbc.execute("TRUNCATE speaker, talk, rating, talk_co_speaker RESTART IDENTITY");
        ada = registerSpeaker("ada@example.com");
        grace = registerSpeaker("grace@example.com");
    }

    @DisplayName("R1, R2, R4, R6: Leaves out one highest and one lowest score from 5 ratings")
    @Description("""
            Each score comes from a different reviewer; 4x39 means 39 reviewers who each gave 4.
            The average is the JSON text of averageRating in GET /talks/{id}; a blank average is JSON null.
            """)
    @TableTest("""
        Scenario                                  | Scores                | Average Rating? | Rating Count?
        No ratings                                | []                    |                 | 0
        R1: one rating                            | [5]                   | 5.0             | 1
        R1: two ratings                           | [4, 5]                | 4.5             | 2
        R1: a third rounds down                   | [1, 1, 2]             | 1.33            | 3
        R1: two thirds round up                   | [1, 2, 2]             | 1.67            | 3
        R1: a quarter keeps two decimals          | [4, 4, 4, 5]          | 4.25            | 4
        R1: no trim at 4 ratings                  | [1, 5, 5, 5]          | 4.0             | 4
        R2: one of each end left out              | [1, 2, 3, 4, 5]       | 3.0             | 5
        R2: only one of the tied top and bottom   | [5, 5, 5, 1, 1]       | 3.67            | 5
        R2: the lowest left out at 5 ratings      | [1, 5, 5, 5, 5]       | 5.0             | 5
        R2: all the same                          | [5, 5, 5, 5, 5]       | 5.0             | 5
        R2: tied bottom                           | [1, 1, 1, 1, 5]       | 1.0             | 5
        R2: six ratings                           | [1, 2, 2, 3, 4, 5]    | 2.75            | 6
        R4: every reviewer counts                 | [5x2, 4x39, 1]        | 4.02            | 42
        """)
    void leavesOutOneHighestAndOneLowestScoreFrom5Ratings(List<String> scores, String averageRating, int ratingCount)
            throws Exception {
        long talk = submitTalk("A", ada);
        rateByDifferentReviewers(talk, scores);
        assertAverageAndCount(talk, averageRating, ratingCount);
    }

    @DisplayName("R3: Rounds the exact average to 2 decimals, ties to even")
    @Description("""
            Each score comes from a different reviewer; 4x39 means 39 reviewers who each gave 4.
            The rows with 42 ratings keep 40 scores; their mean, as a double, lies just off the tie and
            rounds the wrong way. Only the exact mean rounds as the table says.
            """)
    @TableTest("""
        Scenario                             | Scores                | Average Rating? | Rating Count?
        3.125 goes down to the even 3.12     | [5, 4, 3x7, 1]        | 3.12            | 10
        3.375 goes up to the even 3.38       | [5, 4x3, 3x5, 1]      | 3.38            | 10
        4.025 goes to 4.02, a double to 4.03 | [5x2, 4x39, 1]        | 4.02            | 42
        4.475 goes to 4.48, a double to 4.47 | [5x20, 4x21, 1]       | 4.48            | 42
        2.175 goes to 2.18, a double to 2.17 | [5, 3x7, 2x33, 1]     | 2.18            | 42
        1.333... is not a tie                | [1, 1, 2]             | 1.33            | 3
        """)
    void roundsTheExactAverageTo2DecimalsTiesToEven(List<String> scores, String averageRating, int ratingCount)
            throws Exception {
        long talk = submitTalk("A", ada);
        rateByDifferentReviewers(talk, scores);
        assertAverageAndCount(talk, averageRating, ratingCount);
    }

    @DisplayName("R1, R2: Counts a reviewer who rates again once, with the latest score")
    @Description("""
            The ratings are sent in the order of the list.
            """)
    @TableTest("""
        Scenario                                 | Ratings                                                                                                   | Average Rating? | Rating Count?
        Four reviewers stay below the trim       | [[reviewer: 1, score: 1], [reviewer: 1, score: 5], [reviewer: 2, score: 5], [reviewer: 3, score: 5], [reviewer: 4, score: 1]] | 4.0 | 4
        Five reviewers, the latest score trimmed | [[reviewer: 1, score: 5], [reviewer: 1, score: 1], [reviewer: 2, score: 1], [reviewer: 3, score: 3], [reviewer: 4, score: 4], [reviewer: 5, score: 5]] | 2.67 | 5
        One reviewer rates again                 | [[reviewer: 1, score: 2], [reviewer: 1, score: 5]]                                                        | 5.0             | 1
        """)
    void countsAReviewerWhoRatesAgainOnceWithTheLatestScore(List<Rating> ratings, String averageRating,
                                                            int ratingCount) throws Exception {
        long talk = submitTalk("A", ada);
        for (Rating rating : ratings) {
            rate(talk, rating.reviewer(), rating.score());
        }
        assertAverageAndCount(talk, averageRating, ratingCount);
    }

    @DisplayName("R5: Ranks talks by the rounded average")
    @Description("""
            Talks A, B, C and D are submitted in this order, so A is the oldest. Each score comes from a
            different reviewer; 3x7 means 7 reviewers who each gave 3. An empty list means that nobody has
            rated the talk. Ada submits A and B, Grace submits C and D: a speaker submits at most 3
            talks. The talks in Withdrawn are withdrawn after the ratings. The ranking is
            GET /talks?sort=rating.
            """)
    @TableTest("""
        Scenario                                     | Talk A          | Talk B            | Talk C          | Talk D | Withdrawn | Ranking?
        Same rounded average, more ratings first     | [5, 4, 3x7, 1]  | [5, 4x2, 3x15, 1] | []              | []     | []        | [B, A, C, D]
        Same average and count, oldest first         | [4, 5]          | [4, 5]            | []              | []     | []        | [A, B, C, D]
        The trimmed averages                         | [5, 5, 5, 1, 1] | [1, 5, 5, 5]      | [5, 5, 5, 5, 1] | []     | []        | [C, B, A, D]
        Two decimals tell close talks apart          | []              | [4, 4, 4, 5]      | [4, 4, 5]       | []     | []        | [C, B, A, D]
        Unrated last, oldest first                   | []              | [1]               | []              | []     | []        | [B, A, C, D]
        Withdrawn after all others                   | [4, 5]          | [3]               | []              | []     | [A]       | [B, C, D, A]
        """)
    void ranksTalksByTheRoundedAverage(List<String> talkA, List<String> talkB, List<String> talkC,
                                       List<String> talkD, List<String> withdrawn, List<String> ranking)
            throws Exception {
        Map<String, List<String>> scores = Map.of("A", talkA, "B", talkB, "C", talkC, "D", talkD);
        for (String title : List.of("A", "B", "C", "D")) {
            long speaker = List.of("A", "B").contains(title) ? ada : grace;
            long talk = submitTalk(title, speaker);
            rateByDifferentReviewers(talk, scores.get(title));
            if (withdrawn.contains(title)) {
                HttpResponse<String> res = send("POST", "/talks/" + talk + "/withdraw", "{\"speakerId\":" + speaker + "}");
                assertEquals(200, res.statusCode(), res.body());
            }
        }
        assertEquals(ranking, titles(send("GET", "/talks?sort=rating", null)));
    }

    /** One rating in a table: a reviewer and the score they give. */
    record Rating(long reviewer, int score) {
    }

    @TypeConverter
    public static Rating parseRating(Map<String, String> fields) {
        return new Rating(Long.parseLong(fields.get("reviewer")), Integer.parseInt(fields.get("score")));
    }

    /** The scores one per reviewer: {@code 4x39} stands for 39 scores of 4. */
    private static List<Integer> expand(List<String> scores) {
        return scores.stream()
                .flatMap(score -> {
                    String[] parts = score.split("x");
                    int times = parts.length == 2 ? Integer.parseInt(parts[1]) : 1;
                    return Collections.nCopies(times, Integer.parseInt(parts[0])).stream();
                })
                .collect(Collectors.toList());
    }

    private void rateByDifferentReviewers(long talk, List<String> scores) throws Exception {
        List<Integer> each = expand(scores);
        for (int reviewer : IntStream.rangeClosed(1, each.size()).toArray()) {
            rate(talk, reviewer, each.get(reviewer - 1));
        }
    }

    private void rate(long talk, long reviewer, int score) throws Exception {
        HttpResponse<String> res = send("PUT", "/talks/" + talk + "/ratings/" + reviewer, "{\"score\":" + score + "}");
        assertEquals(200, res.statusCode(), res.body());
    }

    /** Compares the JSON text of averageRating, so that {@code 4.0} and {@code 4} differ. */
    private void assertAverageAndCount(long talk, String averageRating, int ratingCount) throws Exception {
        HttpResponse<String> res = send("GET", "/talks/" + talk, null);
        assertEquals(200, res.statusCode(), res.body());
        JsonNode shown = JSON.readTree(res.body());
        assertEquals(averageRating == null ? "null" : averageRating, shown.get("averageRating").toString());
        assertEquals(ratingCount, shown.get("ratingCount").asInt());
    }

    private long registerSpeaker(String email) throws Exception {
        HttpResponse<String> res = send("POST", "/speakers",
                JSON.writeValueAsString(JSON.createObjectNode().put("name", "Ada Lovelace").put("email", email)));
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body()).get("id").asLong();
    }

    private long submitTalk(String title, long speaker) throws Exception {
        HttpResponse<String> res = send("POST", "/talks", JSON.writeValueAsString(JSON.createObjectNode()
                .put("title", title).put("format", "PRESENTATION").put("level", "BEGINNER").put("speakerId", speaker)));
        assertEquals(201, res.statusCode(), res.body());
        return JSON.readTree(res.body()).get("id").asLong();
    }

    private static List<String> titles(HttpResponse<String> res) throws Exception {
        assertEquals(200, res.statusCode(), res.body());
        return StreamSupport.stream(JSON.readTree(res.body()).spliterator(), false)
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
