package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * Speaker registration: a real email, a bio of at most 500 characters, and one speaker per email in any letter case,
 * also under concurrent registrations. Runs the real app on a random port with a throwaway database, emptied before
 * each test.
 */
@DisplayName("Speakers")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpeakersTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Autowired
    JdbcTemplate jdbc;

    private final ApiClient api;

    SpeakersTest(@Value("${local.server.port}") int port) {
        api = new ApiClient(port);
    }

    @BeforeEach
    void startWithNoSpeakers() {
        jdbc.execute("TRUNCATE speaker, talk, rating RESTART IDENTITY");
    }

    @DisplayName("R3: Registers a speaker only with a name")
    @Description("""
            A blank cell sends JSON null as the name. Each row registers with a real email.
            """)
    @TableTest("""
        Scenario    | Name         | Status?
        A real name | Grace Hopper | 201
        Empty       | ''           | 400
        Only spaces | '   '        | 400
        No name     |              | 400
        """)
    void registersASpeakerOnlyWithAName(String name, int status) throws Exception {
        assertEquals(status, api.send("POST", "/speakers", speaker(name, "grace@example.com", null)).statusCode());
    }

    @DisplayName("R3: Registers a speaker only with a real email address")
    @Description("""
            A real address has one @, a local part, and a domain of at least two labels split by dots,
            with no spaces. A blank cell sends JSON null as the email.
            """)
    @TableTest("""
        Scenario                        | Email                               | Status?
        Simple address                  | ada@example.com                     | 201
        Plus sign and subdomains        | ada.lovelace+cfp@mail.example.co.uk | 201
        No @                            | not-an-email                        | 400
        Domain with one label           | ada@example                         | 400
        No local part                   | @example.com                        | 400
        Domain starts with a dot        | ada@.com                            | 400
        Domain ends with a dot          | ada@example.                        | 400
        Two dots in a row in the domain | ada@example..com                    | 400
        Two @                           | ada@@example.com                    | 400
        Space in the local part         | ada lovelace@example.com            | 400
        Empty                           | ''                                  | 400
        Only spaces                     | '   '                               | 400
        No email                        |                                     | 400
        """)
    void registersASpeakerOnlyWithARealEmail(String email, int status) throws Exception {
        assertEquals(status, register(email, null).statusCode());
    }

    @DisplayName("Finds a registered speaker by ID")
    @Description("""
            Each row registers one speaker first, who gets ID 1.
            """)
    @TableTest("""
        Scenario       | Speaker ID | Status?
        Registered     | 1          | 200
        Not registered | 2          | 404
        Not a number   | abc        | 400
        """)
    void findsARegisteredSpeakerById(String speakerId, int status) throws Exception {
        assertEquals(201, register("grace@example.com", null).statusCode());
        assertEquals(status, api.get("/speakers/" + speakerId).statusCode());
    }

    @DisplayName("R7: Treats a trailing slash like the path without it")
    @Description("""
            Each row registers speaker 1 first. Every request then carries the registration of
            another speaker as its body, which GET ignores.
            """)
    @TableTest("""
        Scenario           | Method | Path                        | Status?
        Look up a speaker  | GET    | {/speakers/1, /speakers/1/} | 200
        Register a speaker | POST   | {/speakers, /speakers/}     | 201
        """)
    void treatsATrailingSlashLikeThePathWithoutIt(String method, String path, int status) throws Exception {
        assertEquals(201, register("grace@example.com", null).statusCode());
        assertEquals(status, api.send(method, path, speaker("ada@example.com", null)).statusCode());
    }

    @DisplayName("R4: Limits the bio to 500 characters")
    @Description("""
            A character is one Unicode code point, so the emoji 😀 counts once, although JSON and Java
            count it as two UTF-16 units. A bio is optional.
            """)
    @TableTest("""
        Scenario                 | Bio      | Status?
        No bio                   |          | 201
        At the limit             | 500 × x  | 201
        One past the limit       | 501 × x  | 400
        Emoji at the limit       | 500 × 😀 | 201
        Emoji one past the limit | 501 × 😀 | 400
        """)
    void limitsTheBioTo500Characters(Bio bio, int status) throws Exception {
        HttpResponse<String> res = register("grace@example.com", bio);
        assertEquals(status, res.statusCode(), res.body());
    }

    @DisplayName("R4: Applies the bio limit when an admin updates a speaker")
    @Description("""
            The admin update is PUT /speakers/{id}. Each row updates the bio of a registered speaker
            and keeps the name and the email.
            """)
    @TableTest("""
        Scenario           | Bio      | Status?
        Emoji at the limit | 500 × 😀 | 200
        One past the limit | 501 × x  | 400
        """)
    void limitsTheBioTo500CharactersOnUpdate(Bio bio, int status) throws Exception {
        String id = JSON.readTree(register("grace@example.com", null).body()).get("id").asText();
        HttpResponse<String> res = api.send("PUT", "/speakers/" + id, speaker("grace@example.com", bio));
        assertEquals(status, res.statusCode(), res.body().substring(0, Math.min(200, res.body().length())));
    }

    @DisplayName("R3, R5: Updates a speaker by the rules of registration")
    @Description("""
            The admin update is PUT /speakers/{id}. Each row registers grace@example.com (ID 1) and
            ada@example.com (ID 2) first. The reason is the message field of the error body.
            """)
    @TableTest("""
        Scenario                    | Speaker ID | Name         | Email             | Status? | Message?
        Own email in another case   | 1          | Grace Hopper | GRACE@example.com | 200     |
        Another speaker's email     | 1          | Grace Hopper | ADA@example.com   | 409     | email already registered
        Not an email                | 1          | Grace Hopper | not-an-email      | 400     | invalid email
        No name                     | 1          |              | grace@example.com | 400     | name is required
        Name of only spaces         | 1          | '   '        | grace@example.com | 400     | name is required
        Unknown speaker             | 3          | Grace Hopper | grace@example.com | 404     | speaker not found
        """)
    void updatesASpeakerByTheRulesOfRegistration(String speakerId, String name, String email, int status,
                                                 String message) throws Exception {
        assertEquals(201, register("grace@example.com", null).statusCode());
        assertEquals(201, register("ada@example.com", null).statusCode());
        HttpResponse<String> res = api.send("PUT", "/speakers/" + speakerId, speaker(name, email, null));
        assertEquals(status, res.statusCode(), res.body());
        JsonNode body = JSON.readTree(res.body());
        if (message == null) {
            assertEquals(email, body.get("email").asText());
        } else {
            assertEquals(message, body.get("message").asText());
        }
    }

    @DisplayName("R5: Refuses a second speaker with the same email")
    @Description("""
            Each row registers a second speaker after a first one is registered.
            """)
    @TableTest("""
        Scenario                        | First Email     | Second Email                       | Status?
        Same address in any letter case | ada@example.com | {ada@example.com, ADA@Example.com} | 409
        Another address                 | ada@example.com | grace@example.com                  | 201
        """)
    void refusesASecondSpeakerWithTheSameEmail(String firstEmail, String secondEmail, int status) throws Exception {
        assertEquals(201, register(firstEmail, null).statusCode());
        assertEquals(status, register(secondEmail, null).statusCode());
    }

    @Test
    void concurrentRegistrationsOfOneEmailHaveOneWinner() throws Exception {
        String email = "racer-" + UUID.randomUUID() + "@example.com";
        List<CompletableFuture<HttpResponse<String>>> sent = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String variant = i % 2 == 0 ? email : email.toUpperCase();
            sent.add(api.sendAsync("POST", "/speakers", speaker(variant, null)));
        }
        int created = 0;
        for (CompletableFuture<HttpResponse<String>> f : sent) {
            HttpResponse<String> res = f.join();
            assertTrue(res.statusCode() == 201 || res.statusCode() == 409, res.statusCode() + " " + res.body());
            if (res.statusCode() == 201) {
                created++;
            }
        }
        assertEquals(1, created);
    }

    @Test
    void searchMatchesNamesRegardlessOfCase() throws Exception {
        String name = "Lovelace" + UUID.randomUUID().toString().substring(0, 8);
        assertEquals(201, api.send("POST", "/speakers",
                "{\"name\":\"Ada " + name + "\",\"email\":\"ada-" + UUID.randomUUID() + "@example.com\"}").statusCode());

        HttpResponse<String> found = api.get("/speakers/search?q=" + name.toLowerCase());
        assertEquals(200, found.statusCode());
        assertTrue(found.body().contains("\"name\":\"Ada " + name + "\""), found.body());
    }

    @Test
    void searchTreatsTheTermAsTextNotSql() throws Exception {
        assertEquals(201, api.send("POST", "/speakers", speaker("Conan O'Brien", "conan@example.com", null)).statusCode());

        HttpResponse<String> found = api.get("/speakers/search?q=O%27Brien");
        assertEquals(200, found.statusCode(), found.body());
        assertTrue(found.body().contains("\"name\":\"Conan O'Brien\""), found.body());
    }

    /** A bio written in a table as a count and a character, for example {@code 500 × 😀}. */
    record Bio(String text) {
    }

    @TypeConverter
    public static Bio parseBio(String cell) {
        String[] countAndCharacter = cell.split(" × ");
        return new Bio(countAndCharacter[1].repeat(Integer.parseInt(countAndCharacter[0])));
    }

    private HttpResponse<String> register(String email, Bio bio) throws Exception {
        return api.send("POST", "/speakers", speaker(email, bio));
    }

    private static String speaker(String email, Bio bio) throws Exception {
        return speaker("Grace Hopper", email, bio);
    }

    private static String speaker(String name, String email, Bio bio) throws Exception {
        ObjectNode speaker = JSON.createObjectNode().put("name", name).put("email", email);
        if (bio != null) {
            speaker.put("bio", bio.text());
        }
        return JSON.writeValueAsString(speaker);
    }
}
