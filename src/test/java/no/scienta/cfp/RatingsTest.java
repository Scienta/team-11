package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

/**
 * Rating a talk: who counts as a reviewer, and the average a talk shows. Runs the real app on a random port with a
 * throwaway database.
 */
@DisplayName("Ratings")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RatingsTest {

    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");
    private static final ObjectMapper JSON = new ObjectMapper();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Value("${local.server.port}")
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private String talk;

    @BeforeEach
    void submitATalk() throws Exception {
        String speaker = created(send("POST", "/speakers",
                "{\"name\":\"Barbara Liskov\",\"email\":\"barbara-" + UUID.randomUUID() + "@example.com\"}"));
        talk = created(send("POST", "/talks", "{\"title\":\"Data abstraction\",\"format\":\"PRESENTATION\","
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
        HttpResponse<String> res = send("PUT", "/talks/" + talk + "/ratings/" + reviewer, "{\"score\":4}");
        assertEquals(status, res.statusCode(), res.body());
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
        JsonNode shown = JSON.readTree(send("GET", "/talks/" + talk, null).body());
        assertEquals(averageRating, averageRatingOf(shown));
        assertEquals(ratingCount, shown.get("ratingCount").asInt());
    }

    /** The talk's averageRating, or null when the response has JSON null. */
    private static Double averageRatingOf(JsonNode talk) {
        JsonNode average = talk.get("averageRating");
        return average.isNull() ? null : average.asDouble();
    }

    private void rateByDifferentReviewers(List<Integer> scores) throws Exception {
        for (int reviewer = 1; reviewer <= scores.size(); reviewer++) {
            HttpResponse<String> res = send("PUT", "/talks/" + talk + "/ratings/" + reviewer,
                    "{\"score\":" + scores.get(reviewer - 1) + "}");
            assertEquals(200, res.statusCode(), res.body());
        }
    }

    private String created(HttpResponse<String> res) {
        assertEquals(201, res.statusCode(), res.body());
        Matcher m = ID.matcher(res.body());
        assertTrue(m.find(), res.body());
        return m.group(1);
    }

    private HttpResponse<String> send(String method, String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
