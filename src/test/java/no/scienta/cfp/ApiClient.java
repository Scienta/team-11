package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends JSON requests to the app under test on its random port, as a client of the API would.
 */
final class ApiClient {

    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");

    private final HttpClient http = HttpClient.newHttpClient();
    private final int port;

    ApiClient(int port) {
        this.port = port;
    }

    HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send("GET", path, null);
    }

    HttpResponse<String> send(String method, String path, String json) throws IOException, InterruptedException {
        return http.send(request(method, path, json), HttpResponse.BodyHandlers.ofString());
    }

    CompletableFuture<HttpResponse<String>> sendAsync(String method, String path, String json) {
        return http.sendAsync(request(method, path, json), HttpResponse.BodyHandlers.ofString());
    }

    /** The id in the body of a 201 response. */
    static String createdId(HttpResponse<String> res) {
        assertEquals(201, res.statusCode(), res.body());
        Matcher m = ID.matcher(res.body());
        assertTrue(m.find(), res.body());
        return m.group(1);
    }

    private HttpRequest request(String method, String path, String json) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                .build();
    }
}
