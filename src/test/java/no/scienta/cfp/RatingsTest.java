package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.junit.TypeConverter;

/**
 * Rating a talk: who counts as a reviewer, and the average a talk shows. Runs the real app on a random port with a
 * throwaway database.
 */
@DisplayName("Ratings")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RatingsTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    private final ApiClient api;

    RatingsTest(@Value("${local.server.port}") int port) {
        api = new ApiClient(port);
    }

    private String talk;

    @BeforeEach
    void submitATalk() throws Exception {
        String speaker = ApiClient.createdId(api.send("POST", "/speakers",
                "{\"name\":\"Barbara Liskov\",\"email\":\"barbara-" + UUID.randomUUID() + "@example.com\"}"));
        talk = ApiClient.createdId(api.send("POST", "/talks", "{\"title\":\"Data abstraction\",\"format\":\"PRESENTATION\","
                + "\"level\":\"ADVANCED\",\"speakerId\":" + speaker + "}"));
    }

    @DisplayName("R1: Accepts a reviewer from 1 to 9223372036854775807")
    @Description("""
            Reviewers are not registered: the reviewer is the number at the end of
            PUT /talks/{id}/ratings/{reviewerId}. Each row rates a newly submitted talk with score 4.
            Open question for the product owner: is a reviewer number above 2147483647 realistic?
            """)
    @TableTest("""
        Scenario                    | Reviewer                 | Status?
        Lowest positive number      | 1                        | 200
        Around the largest 32-bit   | {2147483647, 2147483648} | 200
        Largest 64-bit number       | 9223372036854775807      | 200
        One past the largest 64-bit | 9223372036854775808      | 400
        Zero                        | 0                        | 400
        Negative number             | -1                       | 400
        """)
    void acceptsAnyPositiveWholeNumberAsReviewer(String reviewer, int status) throws Exception {
        HttpResponse<String> res = api.send("PUT", "/talks/" + talk + "/ratings/" + reviewer, "{\"score\":4}");
        assertEquals(status, res.statusCode(), res.body());
    }

    @DisplayName("R2: Accepts only a whole number from 1 to 5 as score")
    @Description("""
            The score is the JSON value of "score" in the request body, written here as it is sent.
            A blank cell sends a body without "score". Each row is the first rating of reviewer 1.
            """)
    @TableTest("""
        Scenario             | Score           | Status?
        A score on the scale | {1, 2, 3, 4, 5} | 200
        Below the scale      | 0               | 400
        Above the scale      | 6               | 400
        Decimal number       | 4.0             | 400
        Number as text       | '"4"'           | 400
        Yes or no            | true            | 400
        No score             |                 | 400
        """)
    void acceptsOnlyAWholeNumberFrom1To5AsScore(String score, int status) throws Exception {
        HttpResponse<String> res = api.send("PUT", "/talks/" + talk + "/ratings/1", ratingBody(score));
        assertEquals(status, res.statusCode(), res.body());
    }

    @DisplayName("R3: Keeps one rating per reviewer, the latest score")
    @Description("""
            The ratings are sent in the order of the list. The average and the count are read from
            GET /talks/{id}.
            """)
    @TableTest("""
        Scenario                 | Ratings                                            | Average Rating? | Rating Count?
        One reviewer rates again | [[reviewer: 1, score: 2], [reviewer: 1, score: 5]] | 5.0             | 1
        Two reviewers rate once  | [[reviewer: 1, score: 2], [reviewer: 2, score: 5]] | 3.5             | 2
        """)
    void keepsOneRatingPerReviewer(List<Rating> ratings, Double averageRating, int ratingCount) throws Exception {
        rate(ratings);
        JsonNode shown = JSON.readTree(api.get("/talks/" + talk).body());
        assertEquals(averageRating, averageRatingOf(shown));
        assertEquals(ratingCount, shown.get("ratingCount").asInt());
    }

    @DisplayName("R4: Shows the average score with one decimal, halves up")
    @Description("""
            Each score comes from a different reviewer. The average and the count are read from
            GET /talks/{id}; the other talk responses carry the same two fields.
            """)
    @TableTest("""
        Scenario                   | Scores       | Average Rating? | Rating Count?
        No ratings                 | []           |                 | 0
        Average with one decimal   | [4, 5]       | 4.5             | 2
        Average with a second half | [4, 4, 4, 5] | 4.3             | 4
        """)
    void showsTheAverageScoreWithOneDecimal(List<Integer> scores, Double averageRating, int ratingCount)
            throws Exception {
        rateByDifferentReviewers(scores);
        JsonNode shown = JSON.readTree(api.get("/talks/" + talk).body());
        assertEquals(averageRating, averageRatingOf(shown));
        assertEquals(ratingCount, shown.get("ratingCount").asInt());
    }

    /** The talk's averageRating, or null when the response has JSON null. */
    private static Double averageRatingOf(JsonNode talk) {
        JsonNode average = talk.get("averageRating");
        return average.isNull() ? null : average.asDouble();
    }

    /** One rating in a table: a reviewer and the score they give. */
    record Rating(long reviewer, int score) {
    }

    @TypeConverter
    public static Rating parseRating(Map<String, String> fields) {
        return new Rating(Long.parseLong(fields.get("reviewer")), Integer.parseInt(fields.get("score")));
    }

    private void rate(List<Rating> ratings) throws Exception {
        for (Rating rating : ratings) {
            HttpResponse<String> res = api.send("PUT", "/talks/" + talk + "/ratings/" + rating.reviewer(),
                    ratingBody(String.valueOf(rating.score())));
            assertEquals(200, res.statusCode(), res.body());
        }
    }

    /** A rating request body with the score as raw JSON, or without a score when it is null. */
    private static String ratingBody(String scoreJson) {
        return scoreJson == null ? "{}" : "{\"score\":" + scoreJson + "}";
    }

    private void rateByDifferentReviewers(List<Integer> scores) throws Exception {
        for (int reviewer = 1; reviewer <= scores.size(); reviewer++) {
            HttpResponse<String> res = api.send("PUT", "/talks/" + talk + "/ratings/" + reviewer,
                    "{\"score\":" + scores.get(reviewer - 1) + "}");
            assertEquals(200, res.statusCode(), res.body());
        }
    }
}
