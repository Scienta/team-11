package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

/**
 * Rating a talk: who counts as a reviewer. Runs the real app on a random port with a throwaway database.
 */
@DisplayName("Ratings")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RatingsTest {

    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");

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

    private String created(HttpResponse<String> res) {
        assertEquals(201, res.statusCode(), res.body());
        Matcher m = ID.matcher(res.body());
        assertTrue(m.find(), res.body());
        return m.group(1);
    }

    private HttpResponse<String> send(String method, String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
