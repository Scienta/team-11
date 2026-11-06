package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.junit.TypeConverter;

/**
 * Talk submission: the format sets the duration, only the listed formats and levels are accepted, and a speaker submits
 * at most 3 talks, also when the submissions arrive at the same time. Runs the real app on a random port with a
 * throwaway database.
 */
@DisplayName("Talks")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TalksTest {

    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");
    private static final ObjectMapper JSON = new ObjectMapper();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Value("${local.server.port}")
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @DisplayName("R4: Sets the duration from the format")
    @Description("""
            The speaker does not choose the length. Each row is the first talk of a newly registered
            speaker, at level BEGINNER.
            """)
    @TableTest("""
        Scenario        | Format       | Duration (minutes)?
        Short lightning | LIGHTNING_10 | 10
        Long lightning  | LIGHTNING_20 | 20
        Presentation    | PRESENTATION | 45
        Workshop        | WORKSHOP     | 120
        """)
    void setsTheDurationFromTheFormat(String format, int durationMinutes) throws Exception {
        HttpResponse<String> res = submit(format, "BEGINNER");
        assertEquals(durationMinutes, JSON.readTree(res.body()).get("durationMinutes").asInt(), res.body());
    }

    @DisplayName("R5: Accepts only the four formats, in capitals")
    @Description("""
            Each row is the first talk of a newly registered speaker, at level BEGINNER.
            Keynotes are invited, not submitted.
            """)
    @TableTest("""
        Scenario        | Format                                               | Status?
        A listed format | {LIGHTNING_10, LIGHTNING_20, PRESENTATION, WORKSHOP} | 201
        Keynote         | KEYNOTE                                              | 400
        Lower case      | presentation                                         | 400
        No format       |                                                      | 400
        """)
    void acceptsOnlyTheFourFormats(String format, int status) throws Exception {
        assertEquals(status, submit(format, "BEGINNER").statusCode());
    }

    @DisplayName("R5: Accepts only the three levels, in capitals")
    @Description("""
            Each row is the first talk of a newly registered speaker, in the format PRESENTATION.
            """)
    @TableTest("""
        Scenario       | Level                              | Status?
        A listed level | {BEGINNER, INTERMEDIATE, ADVANCED} | 201
        Another level  | EXPERT                             | 400
        Lower case     | beginner                           | 400
        No level       |                                    | 400
        """)
    void acceptsOnlyTheThreeLevels(String level, int status) throws Exception {
        assertEquals(status, submit("PRESENTATION", level).statusCode());
    }

    @DisplayName("R3: Accepts a talk only with a real title")
    @Description("""
            A blank cell sends JSON null as the title. Each row is the first talk of a newly
            registered speaker, a PRESENTATION at level BEGINNER. A cell such as (200 × x) stands
            for the letter x written 200 times. A character is one Unicode code point, so the emoji
            😀 counts once, although JSON and Java count it as two UTF-16 units.
            """)
    @TableTest("""
        Scenario             | Title               | Status?
        A real title         | Legacy is a feeling | 201
        Empty                | ''                  | 400
        Only spaces          | '   '               | 400
        No title             |                     | 400
        200 characters       | (200 × x)           | 201
        201 characters       | (201 × x)           | 400
        200 emoji characters | (200 × 😀)          | 201
        201 emoji characters | (201 × 😀)          | 400
        """)
    void acceptsATalkOnlyWithARealTitle(TalkTitle title, int status) throws Exception {
        assertEquals(status, submit(title, "PRESENTATION", "BEGINNER").statusCode());
    }

    /** Submits one talk for a newly registered speaker. */
    private HttpResponse<String> submit(String format, String level) throws Exception {
        return submit("Legacy is a feeling", format, level);
    }

    /** A talk title written in a table, where {@code (200 × x)} stands for 200 letters x. */
    record TalkTitle(String text) {
    }

    private static final Pattern REPEATED_CHARACTER = Pattern.compile("\\((\\d+) × (\\X)\\)");

    @TypeConverter
    public static TalkTitle parseTalkTitle(String cell) {
        return new TalkTitle(REPEATED_CHARACTER.matcher(cell)
                .replaceAll(run -> run.group(2).repeat(Integer.parseInt(run.group(1)))));
    }

    private HttpResponse<String> submit(TalkTitle title, String format, String level) throws Exception {
        return submit(title == null ? null : title.text(), format, level);
    }

    private HttpResponse<String> submit(String title, String format, String level) throws Exception {
        String talk = JSON.writeValueAsString(JSON.createObjectNode()
                .put("title", title)
                .put("format", format)
                .put("level", level)
                .put("speakerId", Long.parseLong(newSpeaker())));
        return http.send(request("POST", "/talks", talk), HttpResponse.BodyHandlers.ofString());
    }

    @DisplayName("R7: Keeps a speaker at 3 talks when 10 arrive at the same time")
    @Test
    void concurrentSubmissionsNeverExceedThreeTalks() throws Exception {
        String speaker = newSpeaker();
        List<CompletableFuture<HttpResponse<String>>> sent = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sent.add(http.sendAsync(request("POST", "/talks",
                    "{\"title\":\"Talk " + i + "\",\"format\":\"PRESENTATION\",\"level\":\"BEGINNER\",\"speakerId\":" + speaker + "}"),
                    HttpResponse.BodyHandlers.ofString()));
        }
        int created = 0;
        for (CompletableFuture<HttpResponse<String>> f : sent) {
            HttpResponse<String> res = f.join();
            assertTrue(res.statusCode() == 201 || res.statusCode() == 409, res.statusCode() + " " + res.body());
            if (res.statusCode() == 201) {
                created++;
            }
        }
        assertEquals(3, created);
        HttpResponse<String> talks = http.send(request("GET", "/speakers/" + speaker + "/talks", null),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(3, count(talks.body()));
    }

    private String newSpeaker() throws Exception {
        HttpResponse<String> res = http.send(request("POST", "/speakers",
                "{\"name\":\"Grace Hopper\",\"email\":\"grace-" + UUID.randomUUID() + "@example.com\"}"),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, res.statusCode(), res.body());
        Matcher m = ID.matcher(res.body());
        assertTrue(m.find(), res.body());
        return m.group(1);
    }

    private static int count(String body) {
        Matcher m = ID.matcher(body);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    private HttpRequest request(String method, String path, String json) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                .build();
    }
}
