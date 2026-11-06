package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

/**
 * Answers while the database does not answer at all (security review S17): the client gets the fixed message, never
 * the driver's or the pool's text. Runs the real app on a random port against a database port where nothing listens.
 */
@DisplayName("Database down")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:1/cfp",
        "spring.datasource.hikari.connection-timeout=250",
        "spring.datasource.hikari.initialization-fail-timeout=-1",
        "spring.sql.init.mode=never"})
public class DatabaseDownTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Value("${local.server.port}")
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @DisplayName("Answers without details when the database is down")
    @Description("""
            No database answers on the configured port. A request that starts a transaction fails
            before its first query; a request without a transaction fails on its first query.
            """)
    @TableTest("""
        Scenario                      | Request     | Status? | Message?
        Request with a transaction    | /talks/1    | 500     | the service could not complete the request
        Request without a transaction | /speakers/1 | 500     | the service could not complete the request
        """)
    void answersWithoutDetailsWhenTheDatabaseIsDown(String request, int status, String message) throws Exception {
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + request))
                .GET().build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(status, res.statusCode(), res.body());
        assertEquals(message, JSON.readTree(res.body()).get("message").asText(), res.body());
        assertFalse(res.body().matches("(?s).*(JDBC|Hikari|Exception|PSQL).*"), res.body());
    }
}
