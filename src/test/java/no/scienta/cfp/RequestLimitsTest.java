package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.junit.TypeConverter;

/**
 * Limits on what a request can make the service do (security review, 2026-11-06): the size of a request body, and
 * no multipart parsing. Runs the real app on a random port with a throwaway database, emptied before each test.
 */
@DisplayName("Request limits")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RequestLimitsTest {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationContext context;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void startWithOneSubmittedTalk() throws Exception {
        jdbc.execute("TRUNCATE speaker, talk, rating RESTART IDENTITY");
        send(fixedLength(), "POST", "/speakers", "{\"name\":\"Grace Hopper\",\"email\":\"grace@example.com\"}");
        send(fixedLength(), "POST", "/talks",
                "{\"title\":\"COBOL\",\"format\":\"PRESENTATION\",\"level\":\"BEGINNER\",\"speakerId\":1}");
    }

    @DisplayName("Refuses a request body larger than 1 MB")
    @Description("""
            Each request is PUT /talks/1/status with the decision WAITLISTED for submitted talk 1, padded
            with an unknown field to the body size. 1 MB is 1048576 bytes. A fixed-length body carries
            a Content-Length; a chunked body does not, so the service must count the bytes itself.
            """)
    @TableTest("""
        Scenario          | Body Size (bytes) | Transfer                | Status?
        Exactly 1 MB      | 1048576           | {fixed length, chunked} | 200
        One byte more     | 1048577           | {fixed length, chunked} | 413
        Far more than 1MB | 10485760          | {fixed length, chunked} | 413
        """)
    void refusesARequestBodyLargerThan1Mb(int bodySize, Transfer transfer, int status) throws Exception {
        assertEquals(status, send(transfer, "PUT", "/talks/1/status", decisionOfSize(bodySize)).statusCode());
    }

    @DisplayName("Leaves multipart bodies unparsed")
    @Test
    void leavesMultipartBodiesUnparsed() {
        assertFalse(context.containsBean("multipartResolver"), "a multipart resolver parses multipart bodies");
    }

    /** How the client sends the body: with a Content-Length, or in chunks without one. */
    record Transfer(Function<byte[], HttpRequest.BodyPublisher> publisher) {
    }

    @TypeConverter
    public static Transfer parseTransfer(String name) {
        switch (name) {
            case "fixed length":
                return fixedLength();
            case "chunked":
                return new Transfer(bytes -> HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(bytes)));
            default:
                throw new IllegalArgumentException("unknown transfer " + name);
        }
    }

    private static Transfer fixedLength() {
        return new Transfer(HttpRequest.BodyPublishers::ofByteArray);
    }

    /** {"status":"WAITLISTED","padding":"xx…"}, exactly {@code size} bytes long. */
    private static String decisionOfSize(int size) {
        String empty = "{\"status\":\"WAITLISTED\",\"padding\":\"\"}";
        return empty.replace("\"padding\":\"\"", "\"padding\":\"" + "x".repeat(size - empty.length()) + "\"");
    }

    private HttpResponse<String> send(Transfer transfer, String method, String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .method(method, transfer.publisher().apply(json.getBytes(StandardCharsets.UTF_8)))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
