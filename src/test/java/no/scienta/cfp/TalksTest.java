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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

/**
 * Talk submission: the format sets the duration, only the listed formats and levels are accepted, and a speaker submits
 * at most 3 talks, also when the submissions arrive at the same time. Runs the real app on a random port with a
 * throwaway database.
 */
@DisplayName("Talks")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TalksTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    private final ApiClient api;

    TalksTest(@Value("${local.server.port}") int port) {
        api = new ApiClient(port);
    }

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
            registered speaker, a PRESENTATION at level BEGINNER.
            """)
    @TableTest("""
        Scenario     | Title               | Status?
        A real title | Legacy is a feeling | 201
        Empty        | ''                  | 400
        Only spaces  | '   '               | 400
        No title     |                     | 400
        """)
    void acceptsATalkOnlyWithARealTitle(String title, int status) throws Exception {
        assertEquals(status, submit(title, "PRESENTATION", "BEGINNER").statusCode());
    }

    @DisplayName("R3, R5: Refuses an incomplete talk with the reason")
    @Description("""
            The reason is the message field of the error body. Speaker "registered" is a newly
            registered speaker; an empty Speaker cell leaves speakerId out.
            """)
    @TableTest("""
        Scenario        | Title  | Format       | Level    | Speaker    | Status? | Message?
        No title        |        | PRESENTATION | BEGINNER | registered | 400     | title is required
        Unknown format  | Legacy | KEYNOTE      | BEGINNER | registered | 400     | 'format must be one of [WORKSHOP, PRESENTATION, LIGHTNING_20, LIGHTNING_10]'
        No format       | Legacy |              | BEGINNER | registered | 400     | 'format must be one of [WORKSHOP, PRESENTATION, LIGHTNING_20, LIGHTNING_10]'
        Unknown level   | Legacy | PRESENTATION | EXPERT   | registered | 400     | unknown level
        No speaker      | Legacy | PRESENTATION | BEGINNER |            | 400     | speakerId is required
        Unknown speaker | Legacy | PRESENTATION | BEGINNER | 999999     | 400     | speaker 999999 does not exist
        """)
    void refusesAnIncompleteTalkWithTheReason(String title, String format, String level, String speaker, int status,
                                              String message) throws Exception {
        String talk = JSON.writeValueAsString(JSON.createObjectNode()
                .put("title", title).put("format", format).put("level", level).put("speakerId", speakerIdFor(speaker)));
        HttpResponse<String> res = api.send("POST", "/talks", talk);
        assertEquals(status, res.statusCode(), res.body());
        assertEquals(message, JSON.readTree(res.body()).get("message").asText());
    }

    @Test
    void returnsTheSubmittedTalkWithItsFieldsInOrderAndReadsItBack() throws Exception {
        String speaker = newSpeaker();
        HttpResponse<String> created = api.send("POST", "/talks", "{\"title\":\"Characterise first\",\"format\":\"WORKSHOP\","
                + "\"level\":\"BEGINNER\",\"speakerId\":" + speaker + "}");
        assertEquals(201, created.statusCode(), created.body());
        JsonNode talk = JSON.readTree(created.body());
        assertEquals(List.of("id", "title", "format", "level", "speakerId", "durationMinutes", "status", "submittedAt",
                "submittedOn", "averageRating", "ratingCount", "coSpeakerIds"), fieldNames(talk));
        assertEquals("Characterise first", talk.get("title").asText());
        assertEquals(speaker, talk.get("speakerId").asText());
        assertEquals(120, talk.get("durationMinutes").asInt());
        assertEquals("SUBMITTED", talk.get("status").asText());
        String submittedAt = talk.get("submittedAt").asText();
        assertTrue(submittedAt.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+00:00"), submittedAt);
        assertEquals(submittedAt.substring(0, 10), talk.get("submittedOn").asText());
        assertTrue(talk.get("averageRating").isNull(), created.body());
        assertEquals(0, talk.get("ratingCount").asInt());

        assertEquals(created.body(), api.get("/talks/" + talk.get("id").asText()).body());
    }

    @Test
    void ignoresADurationChosenByTheClient() throws Exception {
        HttpResponse<String> res = api.send("POST", "/talks", "{\"title\":\"Ten minutes\",\"format\":\"LIGHTNING_10\","
                + "\"level\":\"ADVANCED\",\"speakerId\":" + newSpeaker() + ",\"durationMinutes\":90,\"room\":\"A\"}");
        assertEquals(201, res.statusCode(), res.body());
        assertEquals(10, JSON.readTree(res.body()).get("durationMinutes").asInt());
    }

    @Test
    void refusesAFourthTalkWithTheReason() throws Exception {
        String speaker = newSpeaker();
        String talk = "{\"title\":\"Talk\",\"format\":\"PRESENTATION\",\"level\":\"BEGINNER\",\"speakerId\":" + speaker + "}";
        for (int i = 0; i < 3; i++) {
            assertEquals(201, api.send("POST", "/talks", talk).statusCode());
        }
        HttpResponse<String> fourth = api.send("POST", "/talks", talk);
        assertEquals(409, fourth.statusCode(), fourth.body());
        assertEquals("a speaker can submit at most 3 talks", JSON.readTree(fourth.body()).get("message").asText());
    }

    @DisplayName("R2, R8: Answers 404 for a talk or speaker that does not exist")
    @TableTest("""
        Scenario                    | Path                   | Status? | Message?
        Unknown talk                | /talks/999999          | 404     | talk not found
        Talks of an unknown speaker | /speakers/999999/talks | 404     | speaker not found
        """)
    void answersNotFoundForAnUnknownTalkOrSpeaker(String path, int status, String message) throws Exception {
        HttpResponse<String> res = api.get(path);
        assertEquals(status, res.statusCode(), res.body());
        assertEquals(message, JSON.readTree(res.body()).get("message").asText());
    }

    /** A newly registered speaker for "registered", none for an empty cell, else the ID as written. */
    private Long speakerIdFor(String speaker) throws Exception {
        if (speaker == null) {
            return null;
        }
        return Long.valueOf("registered".equals(speaker) ? newSpeaker() : speaker);
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    /** Submits one talk for a newly registered speaker. */
    private HttpResponse<String> submit(String format, String level) throws Exception {
        return submit("Legacy is a feeling", format, level);
    }

    private HttpResponse<String> submit(String title, String format, String level) throws Exception {
        String talk = JSON.writeValueAsString(JSON.createObjectNode()
                .put("title", title)
                .put("format", format)
                .put("level", level)
                .put("speakerId", Long.parseLong(newSpeaker())));
        return api.send("POST", "/talks", talk);
    }

    @DisplayName("R7: Keeps a speaker at 3 talks when 10 arrive at the same time")
    @Test
    void concurrentSubmissionsNeverExceedThreeTalks() throws Exception {
        String speaker = newSpeaker();
        List<CompletableFuture<HttpResponse<String>>> sent = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sent.add(api.sendAsync("POST", "/talks",
                    "{\"title\":\"Talk " + i + "\",\"format\":\"PRESENTATION\",\"level\":\"BEGINNER\",\"speakerId\":" + speaker + "}"));
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
        assertEquals(3, JSON.readTree(api.get("/speakers/" + speaker + "/talks").body()).size());
    }

    private String newSpeaker() throws Exception {
        return ApiClient.createdId(api.send("POST", "/speakers",
                "{\"name\":\"Grace Hopper\",\"email\":\"grace-" + UUID.randomUUID() + "@example.com\"}"));
    }
}
