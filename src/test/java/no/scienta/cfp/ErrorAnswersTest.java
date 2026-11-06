package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;
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
 * Error answers that must not show the database's details (security review, 2026-11-06): a text with a NUL
 * character, and a failure in the database. Runs the real app on a random port with a throwaway database, emptied
 * before each test.
 */
@DisplayName("Error answers")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ErrorAnswersTest {

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

    @BeforeEach
    void startWithOneSpeaker() throws Exception {
        jdbc.execute("TRUNCATE speaker, talk, rating, talk_co_speaker RESTART IDENTITY");
        send("POST", "/speakers", "{\"name\":\"Grace Hopper\",\"email\":\"grace@example.com\"}");
    }

    @DisplayName("Refuses a text field that holds a NUL character")
    @Description("""
            Speaker 1 is registered. The text is written as it stands in the JSON body, so \\u0000 is
            the JSON escape for the NUL character. The database cannot store that character.
            """)
    @TableTest("""
        Scenario      | Text Field                              | Text               | Status?
        Ordinary text | {speaker name, speaker bio, talk title} | Ada Lovelace       | 201
        NUL character | {speaker name, speaker bio, talk title} | Ada\\u0000Lovelace | 400
        """)
    void refusesATextFieldWithANulCharacter(TextField field, String text, int status) throws Exception {
        assertEquals(status, send("POST", field.path(), field.body(text)).statusCode());
    }

    @DisplayName("Answers a database failure without its details")
    @Description("""
            The search term reaches the database as a parameter, and the database refuses the NUL
            character (%00 in the URL). The answer has the usual error fields.
            """)
    @TableTest("""
        Scenario                         | Request                        | Status? | Message?
        Search term with a NUL character | /speakers/search?q=Grace%00Hop | 500     | the service could not complete the request
        """)
    void answersADatabaseFailureWithoutItsDetails(String request, int status, String message) throws Exception {
        HttpResponse<String> res = send("GET", request, null);
        assertEquals(status, res.statusCode(), res.body());
        assertEquals(message, JSON.readTree(res.body()).get("message").asText(), res.body());
    }

    /** A text field of a request body, with the request that carries it. */
    record TextField(String path, String bodyTemplate) {

        private static final Map<String, TextField> BY_NAME = Map.of(
                "speaker name", new TextField("/speakers", "{\"name\":\"%s\",\"email\":\"ada@example.com\"}"),
                "speaker bio", new TextField("/speakers",
                        "{\"name\":\"Ada Lovelace\",\"email\":\"ada@example.com\",\"bio\":\"%s\"}"),
                "talk title", new TextField("/talks",
                        "{\"title\":\"%s\",\"format\":\"PRESENTATION\",\"level\":\"BEGINNER\",\"speakerId\":1}"));

        String body(String text) {
            return String.format(bodyTemplate, text);
        }
    }

    @TypeConverter
    public static TextField parseTextField(String name) {
        return TextField.BY_NAME.get(name);
    }

    private HttpResponse<String> send(String method, String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
