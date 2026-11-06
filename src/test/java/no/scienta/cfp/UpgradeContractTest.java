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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.SpringVersion;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

/**
 * What a framework upgrade can change without anyone noticing: trailing slashes, JSON formats and property order,
 * request binding, error bodies and the versions on /actuator/info (workflow/specs/spring-boot-4/spec.md). The
 * bodies are checked as text, so the order of the properties counts. Runs the real app on a random port with a
 * throwaway database.
 */
@DisplayName("Spring Boot 4 upgrade")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UpgradeContractTest {

    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");
    private static final String JSON_UTF8 = "application/json;charset=UTF-8";
    private static final String TIMESTAMP = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+00:00";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Value("${local.server.port}")
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private String speakerEmail;
    private String speaker;
    private String talk;

    @BeforeEach
    void registerASpeakerWithATalk() throws Exception {
        speakerEmail = newEmail();
        speaker = created(send("POST", "/speakers", "{\"name\":\"Grace Hopper\",\"email\":\"" + speakerEmail + "\"}"));
        talk = created(send("POST", "/talks", talkJson(speaker)));
    }

    @DisplayName("R2: Answers a path with a trailing slash like the path without it")
    @Description("""
            Speakers R7 names /speakers/ and /speakers/{id}/. Spring Boot 2.7 matched the slash on
            every path, so every path keeps doing so. $speaker and $talk are a registered speaker
            and their talk, $email a new address.
            """)
    @TableTest("""
        Scenario               | Method | Path                           | Body                                                                            | Status?
        Register a speaker     | POST   | /speakers/                     | '{"name":"Grace Hopper","email":"$email"}'                                      | 201
        Read a speaker         | GET    | /speakers/$speaker/            |                                                                                 | 200
        Update a speaker       | PUT    | /speakers/$speaker/            | '{"name":"Grace Hopper","email":"$email"}'                                      | 200
        A speaker's talks      | GET    | /speakers/$speaker/talks/      |                                                                                 | 200
        Submit a talk          | POST   | /talks/                        | '{"title":"T","format":"PRESENTATION","level":"BEGINNER","speakerId":$speaker}' | 201
        Read a talk            | GET    | /talks/$talk/                  |                                                                                 | 200
        List the talks         | GET    | /talks/                        |                                                                                 | 200
        List without withdrawn | GET    | /talks/?includeWithdrawn=false |                                                                                 | 200
        Rate a talk            | PUT    | /talks/$talk/ratings/7/        | '{"score":3}'                                                                   | 200
        Decide on a talk       | PUT    | /talks/$talk/status/           | '{"status":"WAITLISTED"}'                                                       | 200
        Withdraw a talk        | POST   | /talks/$talk/withdraw/         | '{"speakerId":$speaker}'                                                        | 200
        """)
    void answersAPathWithATrailingSlash(String method, String path, String body, int status) throws Exception {
        HttpResponse<String> res = send(method, filledIn(path), body == null ? null : filledIn(body));
        assertEquals(status, res.statusCode(), res.body());
    }

    @DisplayName("R2: Answers every error as JSON in UTF-8, with the Boot error body")
    @Description("""
            As on Boot 2.7, an error has Content-Type application/json;charset=UTF-8, like a success,
            and the fields timestamp, status, error, message and path. $speaker and $talk are a
            registered speaker and their talk, $email a new address and $taken the speaker's address.
            The 500 comes from the unescaped search (tech-debt B1); when B1 is fixed, drop that row.
            """)
    @TableTest("""
        Scenario                   | Method | Path                   | Body                                    | Status?
        Our own 400                | POST   | /speakers              | '{"name":"Ada","email":"not-an-email"}' | 400
        Validation error           | POST   | /speakers              | '{"name":"","email":"$email"}'          | 400
        Malformed JSON             | POST   | /speakers              | '{not json'                             | 400
        Path variable not a number | GET    | /speakers/abc          |                                         | 400
        Not the main speaker       | POST   | /talks/$talk/withdraw  | '{"speakerId":999999}'                  | 403
        Unknown speaker            | GET    | /speakers/999999       |                                         | 404
        Unknown path               | GET    | /nope                  |                                         | 404
        Wrong method               | DELETE | /speakers              |                                         | 405
        Email taken                | POST   | /speakers              | '{"name":"Ada","email":"$taken"}'       | 409
        Server error               | GET    | /speakers/search?q=%27 |                                         | 500
        """)
    void answersEveryErrorAsJsonInUtf8(String method, String path, String body, int status) throws Exception {
        HttpResponse<String> res = send(method, filledIn(path), body == null ? null : filledIn(body));
        assertEquals(status, res.statusCode(), res.body());
        assertEquals(JSON_UTF8, res.headers().firstValue("Content-Type").orElse(""));
        assertMatches("\\{\"timestamp\":\"" + TIMESTAMP + "\",\"status\":" + status + ",\"error\":\"[^\"]+\","
                + "\"message\":\".*\",(\"errors\":\\[.*],)?\"path\":\"[^\"]+\"}", res.body());
    }

    @Test
    @DisplayName("R2: Shows the same speaker with and without a trailing slash")
    void showsTheSameSpeakerWithATrailingSlash() throws Exception {
        assertEquals(send("GET", "/speakers/" + speaker, null).body(),
                send("GET", "/speakers/" + speaker + "/", null).body());
    }

    @Test
    @DisplayName("R2: Keeps the trailing slash in the path of an error body")
    void keepsTheTrailingSlashInTheErrorPath() throws Exception {
        HttpResponse<String> res = send("GET", "/speakers/999999/", null);
        assertEquals(404, res.statusCode(), res.body());
        assertTrue(res.body().contains("\"path\":\"/speakers/999999/\""), res.body());
    }

    @Test
    @DisplayName("R2: Shows a talk with its fields in order, the old date formats and a null average")
    void showsATalkInTheOldFormat() throws Exception {
        HttpResponse<String> res = send("GET", "/talks/" + talk, null);
        assertEquals(JSON_UTF8, res.headers().firstValue("Content-Type").orElse(""));
        assertMatches("\\{\"id\":" + talk + ",\"title\":\"Legacy is a feeling\",\"format\":\"PRESENTATION\","
                + "\"level\":\"BEGINNER\",\"speakerId\":" + speaker + ",\"durationMinutes\":45,\"status\":\"SUBMITTED\","
                + "\"submittedAt\":\"" + TIMESTAMP + "\",\"submittedOn\":\"\\d{4}-\\d{2}-\\d{2}\","
                + "\"averageRating\":null,\"ratingCount\":0,\"coSpeakerIds\":\\[]}", res.body());
    }

    @Test
    @DisplayName("R2: Shows a speaker with its fields in order and a null bio")
    void showsASpeakerInTheOldFormat() throws Exception {
        assertMatches("\\{\"id\":" + speaker + ",\"name\":\"Grace Hopper\",\"email\":\"grace-[0-9a-f-]+@example.com\","
                + "\"bio\":null}", send("GET", "/speakers/" + speaker, null).body());
    }

    @Test
    @DisplayName("R2: Answers a rating with its fields in order and an ordinary space before AM or PM")
    void answersARatingInTheOldFormat() throws Exception {
        assertMatches("\\{\"talkId\":" + talk + ",\"reviewerId\":7,\"score\":3,"
                        + "\"ratedAtDisplay\":\"\\d{1,2}/\\d{1,2}/\\d{2}, \\d{1,2}:\\d{2} [AP]M\"}",
                send("PUT", "/talks/" + talk + "/ratings/7", "{\"score\":3}").body());
    }

    @Test
    @DisplayName("R2: Answers an error with the Boot error body, not Problem Details")
    void answersAnErrorWithTheBootErrorBody() throws Exception {
        HttpResponse<String> res = send("GET", "/speakers/999999", null);
        assertEquals(404, res.statusCode());
        assertEquals(JSON_UTF8, res.headers().firstValue("Content-Type").orElse(""));
        assertMatches("\\{\"timestamp\":\"" + TIMESTAMP + "\",\"status\":404,\"error\":\"Not Found\","
                + "\"message\":\"speaker not found\",\"path\":\"/speakers/999999\"}", res.body());
    }

    @Test
    @DisplayName("R2: Answers a validation error with the message and the binding errors")
    void answersAValidationErrorWithTheBindingErrors() throws Exception {
        HttpResponse<String> res = send("POST", "/speakers",
                "{\"name\":\"\",\"email\":\"grace-" + UUID.randomUUID() + "@example.com\"}");
        assertEquals(400, res.statusCode());
        assertMatches("\\{\"timestamp\":\"" + TIMESTAMP + "\",\"status\":400,\"error\":\"Bad Request\","
                + "\"message\":\"Validation failed for object='speakerForm'. Error count: 1\",\"errors\":\\[.*"
                + "\"field\":\"name\".*],\"path\":\"/speakers\"}", res.body());
    }

    @DisplayName("R2: Takes only a JSON whole number as a score")
    @Description("""
            Ratings R2. The rating body is a map, so Jackson hands over the JSON type as it is and
            never coerces it. Each row rates the talk as reviewer 7.
            """)
    @TableTest("""
        Scenario     | Score | Status? | Message?
        Whole number | 3     | 200     |
        Decimal      | 3.0   | 400     | score must be a whole number
        Exponent     | 3e0   | 400     | score must be a whole number
        Text         | '"3"' | 400     | score must be a whole number
        Boolean      | true  | 400     | score must be a whole number
        Null         | null  | 400     | score must be a whole number
        Array        | '[3]' | 400     | score must be a whole number
        Out of range | 6     | 400     | score must be 1-5
        """)
    void takesOnlyAWholeNumberAsAScore(String score, int status, String message) throws Exception {
        HttpResponse<String> res = send("PUT", "/talks/" + talk + "/ratings/7", "{\"score\":" + score + "}");
        assertEquals(status, res.statusCode(), res.body());
        if (message != null) {
            assertTrue(res.body().contains("\"message\":\"" + message + "\""), res.body());
        }
    }

    @DisplayName("R2: Reads request bodies as leniently as before")
    @Description("""
            Unknown fields are ignored, comments are allowed (the mobile client sends them), and a
            scalar of the wrong JSON type is coerced to the field's type. $email is a new address and
            $speaker a registered speaker.
            """)
    @TableTest("""
        Scenario                   | Path      | Body                                                                                             | Status?
        Unknown field in a speaker | /speakers | '{"name":"Grace Hopper","email":"$email","twitter":"@grace"}'                                   | 201
        Comment in a speaker       | /speakers | '{/* from the app */ "name":"Grace Hopper","email":"$email"}'                                    | 201
        Unknown field in a talk    | /talks    | '{"title":"Legacy","format":"PRESENTATION","level":"BEGINNER","speakerId":$speaker,"room":"A"}' | 201
        Speaker ID as text         | /talks    | '{"title":"Legacy","format":"PRESENTATION","level":"BEGINNER","speakerId":"$speaker"}'          | 201
        Title as a number          | /talks    | '{"title":5,"format":"PRESENTATION","level":"BEGINNER","speakerId":$speaker}'                   | 201
        """)
    void readsRequestBodiesLeniently(String path, String body, int status) throws Exception {
        String json = body.replace("$email", "grace-" + UUID.randomUUID() + "@example.com").replace("$speaker", speaker);
        HttpResponse<String> res = send("POST", path, json);
        assertEquals(status, res.statusCode(), res.body());
    }

    @DisplayName("R1: Runs on Spring Boot 4 and Java 25")
    @Description("""
            The major version of each field in versions on GET /actuator/info.
            """)
    @TableTest("""
        Field           | Major Version?
        springBoot      | 4
        springFramework | 7
        java            | 25
        """)
    void runsOnSpringBoot4AndJava25(String field, int majorVersion) throws Exception {
        String info = send("GET", "/actuator/info", null).body();
        Matcher version = Pattern.compile("\"" + field + "\":\"(\\d+)").matcher(info);
        assertTrue(version.find(), info);
        assertEquals(majorVersion, Integer.parseInt(version.group(1)), info);
    }

    @Test
    @DisplayName("R3: Reports the running versions on /actuator/info")
    void reportsTheRunningVersions() throws Exception {
        HttpResponse<String> res = send("GET", "/actuator/info", null);
        assertEquals(200, res.statusCode(), res.body());
        assertEquals("{\"versions\":{\"java\":\"" + System.getProperty("java.version") + "\","
                + "\"springBoot\":\"" + SpringBootVersion.getVersion() + "\","
                + "\"springFramework\":\"" + SpringVersion.getVersion() + "\"}}", res.body());
    }

    private static void assertMatches(String regex, String actual) {
        assertTrue(actual.matches(regex), actual + "\n does not match\n" + regex);
    }

    private static String newEmail() {
        return "grace-" + UUID.randomUUID() + "@example.com";
    }

    /** A table cell with $speaker, $talk, $taken (the speaker's address) and $email (a new address) filled in. */
    private String filledIn(String cell) {
        return cell.replace("$speaker", speaker).replace("$talk", talk).replace("$taken", speakerEmail)
                .replace("$email", newEmail());
    }

    private static String talkJson(String speaker) {
        return "{\"title\":\"Legacy is a feeling\",\"format\":\"PRESENTATION\",\"level\":\"BEGINNER\","
                + "\"speakerId\":" + speaker + "}";
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
