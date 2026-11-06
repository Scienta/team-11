package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The shape of an error body, which clients read: Spring Boot's default error attributes, with the message and the
 * binding errors included. Runs the real app on a random port with a throwaway database.
 */
@DisplayName("Error responses")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorResponsesTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    private final ApiClient api;

    ErrorResponsesTest(@Value("${local.server.port}") int port) {
        api = new ApiClient(port);
    }

    @Test
    void aRefusedRequestHasStatusErrorMessageAndPath() throws Exception {
        HttpResponse<String> res = api.get("/talks/999999");

        JsonNode body = JSON.readTree(res.body());
        assertEquals(List.of("timestamp", "status", "error", "message", "path"), JsonFields.namesOf(body));
        assertTimestamp(body);
        assertEquals(404, body.get("status").asInt());
        assertEquals("Not Found", body.get("error").asText());
        assertEquals("talk not found", body.get("message").asText());
        assertEquals("/talks/999999", body.get("path").asText());
    }

    @Test
    void anInvalidSpeakerFormAlsoHasTheBindingErrors() throws Exception {
        HttpResponse<String> res = api.send("POST", "/speakers", "{\"email\":\"ada@example.com\"}");

        JsonNode body = JSON.readTree(res.body());
        assertEquals(List.of("timestamp", "status", "error", "message", "errors", "path"), JsonFields.namesOf(body));
        assertTimestamp(body);
        assertEquals(400, body.get("status").asInt());
        assertEquals("Bad Request", body.get("error").asText());
        assertEquals("Validation failed for object='speakerForm'. Error count: 1", body.get("message").asText());
        assertEquals("name", body.get("errors").get(0).get("field").asText());
        assertEquals("NotBlank", body.get("errors").get(0).get("code").asText());
        assertEquals("/speakers", body.get("path").asText());
    }

    private static void assertTimestamp(JsonNode body) {
        String timestamp = body.get("timestamp").asText();
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+00:00"), timestamp);
    }
}
