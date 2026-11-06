package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

/**
 * Ratings that arrive at the same time (ratings R7): none gets lost and none counts twice. Runs the real app on a
 * random port with a throwaway database.
 */
@DisplayName("Ratings: ratings at the same time")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RatingConcurrencyTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Value("${local.server.port}")
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private long talk;

    @BeforeEach
    void submitATalk() throws Exception {
        long speaker = JSON.readTree(send("POST", "/speakers", "{\"name\":\"Barbara Liskov\",\"email\":\"barbara-"
                + UUID.randomUUID() + "@example.com\"}").join().body()).get("id").asLong();
        talk = JSON.readTree(send("POST", "/talks", "{\"title\":\"Data abstraction\",\"format\":\"PRESENTATION\","
                + "\"level\":\"ADVANCED\",\"speakerId\":" + speaker + "}").join().body()).get("id").asLong();
    }

    @DisplayName("R7: Counts each reviewer once when ratings arrive together")
    @Description("""
            All requests of a row are sent at the same time to one newly submitted talk, each with
            score 4. Reviewers are numbered from 1. A copy is the same rating sent again, as from a
            double-click. The service uses at most 5 database connections.
            """)
    @TableTest("""
        Scenario                    | Reviewers | Copies Each | Answers? | Rating Count?
        Many reviewers at once      | 20        | 1           | {200}    | 20
        One reviewer double-clicks  | 1         | 2           | {200}    | 1
        Many reviewers double-click | 10        | 2           | {200}    | 10
        """)
    void countsEachReviewerOnce(int reviewers, int copiesEach, Set<Integer> answers, int ratingCount)
            throws Exception {
        assertEquals(answers, statusesOf(rateAtTheSameTime(reviewers, copiesEach)));
        assertEquals(ratingCount, JSON.readTree(send("GET", "/talks/" + talk, null).join().body())
                .get("ratingCount").asInt());
    }

    /** Sends every copy of every reviewer's rating without waiting for an answer in between. */
    private List<CompletableFuture<HttpResponse<String>>> rateAtTheSameTime(int reviewers, int copiesEach) {
        return LongStream.rangeClosed(1, reviewers)
                .boxed()
                .flatMap(reviewer -> LongStream.range(0, copiesEach).mapToObj(copy -> reviewer))
                .map(reviewer -> send("PUT", "/talks/" + talk + "/ratings/" + reviewer, "{\"score\":4}"))
                .collect(Collectors.toList());
    }

    private static Set<Integer> statusesOf(List<CompletableFuture<HttpResponse<String>>> sent) {
        return sent.stream().map(CompletableFuture::join).map(HttpResponse::statusCode).collect(Collectors.toSet());
    }

    private CompletableFuture<HttpResponse<String>> send(String method, String path, String json) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
