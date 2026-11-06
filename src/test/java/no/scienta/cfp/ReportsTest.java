package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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

/**
 * The programme committee's reports, /admin/stats and /admin/talks.csv. They are in no spec; these tests pin what
 * they answer today. Runs the real app on a random port with a throwaway database, emptied before each test.
 */
@DisplayName("Reports")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReportsTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Autowired
    JdbcTemplate jdbc;

    private final ApiClient api;

    ReportsTest(@Value("${local.server.port}") int port) {
        api = new ApiClient(port);
    }

    @BeforeEach
    void startWithNoSpeakers() {
        jdbc.execute("TRUNCATE speaker, talk, rating, talk_co_speaker RESTART IDENTITY");
    }

    @Test
    void sumsUpSpeakersAndTalksInStats() throws Exception {
        String speaker = newSpeaker("Grace Hopper");
        newSpeaker("Ada Lovelace");
        submitTalk(speaker, "Compilers", "WORKSHOP");
        submitTalk(speaker, "Nanoseconds", "PRESENTATION");

        JsonNode stats = JSON.readTree(api.get("/admin/stats").body());

        assertEquals(List.of("speakers", "speakersWithTalks", "talks", "byFormat", "byLevel", "byStatus", "totalHours",
                "averageRating", "generated"), JsonFields.namesOf(stats));
        assertEquals(2, stats.get("speakers").asInt());
        assertEquals(1, stats.get("speakersWithTalks").asInt());
        assertEquals(2, stats.get("talks").asInt());
        assertEquals("{\"WORKSHOP\":1,\"PRESENTATION\":1}", stats.get("byFormat").toString());
        assertEquals("{\"BEGINNER\":2}", stats.get("byLevel").toString());
        assertEquals("{\"SUBMITTED\":2}", stats.get("byStatus").toString());
        assertTrue(stats.get("averageRating").isNull(), stats.toString());
        String generated = stats.get("generated").asText();
        assertTrue(generated.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"), generated);
    }

    @DisplayName("Counts the total length of all talks in whole hours, rounded")
    @Description("""
            One speaker submits a talk in each format of the list. Half an hour rounds up.
            """)
    @TableTest("""
        Scenario          | Formats                                  | Total Hours?
        No talks          | []                                       | 0
        185 minutes       | [WORKSHOP, PRESENTATION, LIGHTNING_20]   | 3
        210 minutes       | [WORKSHOP, PRESENTATION, PRESENTATION]   | 4
        160 minutes       | [WORKSHOP, LIGHTNING_20, LIGHTNING_20]   | 3
        150 minutes       | [WORKSHOP, LIGHTNING_20, LIGHTNING_10]   | 3
        140 minutes       | [WORKSHOP, LIGHTNING_10, LIGHTNING_10]   | 2
        """)
    void countsTheTotalLengthInWholeHours(List<String> formats, int totalHours) throws Exception {
        String speaker = newSpeaker("Grace Hopper");
        for (String format : formats) {
            submitTalk(speaker, "A talk", format);
        }
        assertEquals(totalHours, JSON.readTree(api.get("/admin/stats").body()).get("totalHours").asInt());
    }

    @DisplayName("Averages the talks' shown averages, one decimal, halves up")
    @Description("""
            One speaker submits a talk for each list of scores; each score comes from a different
            reviewer. A talk nobody rated does not count.
            """)
    @TableTest("""
        Scenario                     | Scores of each talk | Average Rating?
        No ratings                   | [[]]                |
        One talk                     | [[4, 5]]            | 4.5
        Averages 4.0 and 4.5         | [[4], [4, 5]]       | 4.3
        An unrated talk is left out  | [[4], []]           | 4.0
        """)
    void averagesTheTalksAverages(List<List<Integer>> scoresOfEachTalk, Double averageRating) throws Exception {
        String speaker = newSpeaker("Grace Hopper");
        for (List<Integer> scores : scoresOfEachTalk) {
            String talk = submitTalk(speaker, "A talk", "PRESENTATION");
            for (int reviewer = 1; reviewer <= scores.size(); reviewer++) {
                HttpResponse<String> res = api.send("PUT", "/talks/" + talk + "/ratings/" + reviewer,
                        "{\"score\":" + scores.get(reviewer - 1) + "}");
                assertEquals(200, res.statusCode(), res.body());
            }
        }
        JsonNode average = JSON.readTree(api.get("/admin/stats").body()).get("averageRating");
        assertEquals(averageRating, average.isNull() ? null : average.asDouble());
    }

    @Test
    void exportsTalksAsSemicolonCsv() throws Exception {
        String speaker = newSpeaker("Grace Hopper");
        String talk = submitTalk(speaker, "Semi; colon \"quoted\"", "LIGHTNING_20");
        api.send("PUT", "/talks/" + talk + "/ratings/1", "{\"score\":4}");
        api.send("PUT", "/talks/" + talk + "/ratings/2", "{\"score\":5}");

        HttpResponse<String> csv = api.get("/admin/talks.csv");

        assertEquals(200, csv.statusCode(), csv.body());
        assertEquals("text/csv;charset=UTF-8", csv.headers().firstValue("Content-Type").orElseThrow());
        assertEquals("attachment; filename=talks.csv", csv.headers().firstValue("Content-Disposition").orElseThrow());
        String[] lines = csv.body().split("\n");
        assertEquals(2, lines.length, csv.body());
        assertEquals("id;title;format;minutes;level;speaker;status;submitted;rating;ratings", lines[0]);
        assertTrue(lines[1].matches(talk + ";\"Semi; colon 'quoted'\";Lightning 20;20;Beginner;Grace Hopper;SUBMITTED;"
                + "\\d{1,2}\\. \\p{L}+\\.? \\d{4};4,5;2"), lines[1]);
    }

    private String newSpeaker(String name) throws Exception {
        return ApiClient.createdId(api.send("POST", "/speakers",
                JSON.createObjectNode().put("name", name).put("email", "s-" + UUID.randomUUID() + "@example.com").toString()));
    }

    private String submitTalk(String speaker, String title, String format) throws Exception {
        return ApiClient.createdId(api.send("POST", "/talks", JSON.createObjectNode().put("title", title)
                .put("format", format).put("level", "BEGINNER").put("speakerId", Long.parseLong(speaker)).toString()));
    }
}
