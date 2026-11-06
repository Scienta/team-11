package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
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
        jdbc.execute("TRUNCATE speaker, talk, rating, talk_co_speaker RESTART IDENTITY");
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
            with no spaces. The limits on length come from RFC 5321. A blank cell sends JSON null as
            the email. A cell such as (64 × a) stands for the letter a written 64 times.
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
        Local part of 64 characters     | (64 × a)@example.com                | 201
        Local part of 65 characters     | (65 × a)@example.com                | 400
        Domain of 255 characters        | ada@(251 × e).com                   | 201
        Domain of 256 characters        | ada@(252 × e).com                   | 400
        Empty                           | ''                                  | 400
        Only spaces                     | '   '                               | 400
        No email                        |                                     | 400
        """)
    void registersASpeakerOnlyWithARealEmail(EmailAddress email, int status) throws Exception {
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

    @DisplayName("Refuses to change a registered speaker")
    @Description("""
            Speaker 1 registers as Grace Hopper. The request asks to change the name to Ada Lovelace.
            No spec has an endpoint to change a speaker, and the old admin update had no
            authorisation (security review, 2026-11-06).
            """)
    @TableTest("""
        Scenario      | Path         | Status? | Name After?
        Without slash | /speakers/1  | 405     | Grace Hopper
        With slash    | /speakers/1/ | 405     | Grace Hopper
        """)
    void refusesToChangeARegisteredSpeaker(String path, int status, String nameAfter) throws Exception {
        assertEquals(201, register("grace@example.com", null).statusCode());
        assertEquals(status, api.send("PUT", path, speaker("Ada Lovelace", "grace@example.com", null)).statusCode());
        assertEquals(nameAfter, JSON.readTree(api.get("/speakers/1").body()).get("name").asText());
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

    @DisplayName("Has no speaker search")
    @Description("""
            Security review S18: GET /speakers/search was anonymous, in no spec, and returned speakers
            with their email. The path now means the speaker with the id "search", which is not a
            number. A speaker named Ada Lovelace is registered first, and must not show in the answer.
            """)
    @TableTest("""
        Scenario           | Request                | Status?
        Empty search term  | /speakers/search?q=    | 400
        A name as the term | /speakers/search?q=ada | 400
        """)
    void hasNoSpeakerSearch(String request, int status) throws Exception {
        api.send("POST", "/speakers", speaker("Ada Lovelace", "ada-" + UUID.randomUUID() + "@example.com", null));

        HttpResponse<String> answer = api.get(request);

        assertEquals(status, answer.statusCode(), answer.body());
        assertFalse(answer.body().contains("Ada Lovelace"), answer.body());
    }

    /** A bio written in a table as a count and a character, for example {@code 500 × 😀}. */
    record Bio(String text) {
    }

    @TypeConverter
    public static Bio parseBio(String cell) {
        String[] countAndCharacter = cell.split(" × ");
        return new Bio(countAndCharacter[1].repeat(Integer.parseInt(countAndCharacter[0])));
    }

    /** An email address written in a table, where {@code (64 × a)} stands for 64 letters a. */
    record EmailAddress(String text) {
    }

    private static final Pattern REPEATED_CHARACTER = Pattern.compile("\\((\\d+) × (.)\\)");

    @TypeConverter
    public static EmailAddress parseEmailAddress(String cell) {
        return new EmailAddress(REPEATED_CHARACTER.matcher(cell)
                .replaceAll(run -> run.group(2).repeat(Integer.parseInt(run.group(1)))));
    }

    private HttpResponse<String> register(EmailAddress email, Bio bio) throws Exception {
        return register(email == null ? null : email.text(), bio);
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
