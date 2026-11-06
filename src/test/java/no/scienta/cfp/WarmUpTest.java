package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

/**
 * The warm-up at start-up (workflow/specs/warm-up/spec.md): which requests the service sends to itself, that they
 * change no data, and the log line at the end. Reads the service's own log output, because the request log is also how
 * we check the warm-up on the deployed service. The test sends no HTTP requests of its own.
 */
@DisplayName("Warm-up")
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WarmUpTest {

    private static final Duration WARM_UP_LIMIT = Duration.ofSeconds(10);
    private static final Pattern WARM_UP_DONE = Pattern.compile("Warm-up done: (\\d+) requests in \\d+ ms");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Autowired
    JdbcTemplate jdbc;

    /** The service starts once for this class, inside the first test, so only that test's capture holds the start. */
    private static String startUpLog;

    @BeforeEach
    void keepTheStartUpLog(CapturedOutput output) throws InterruptedException {
        if (startUpLog == null) {
            startUpLog = logUntilTheWarmUpIsDone(output);
        }
    }

    @DisplayName("R1: Sends each warm-up request through the HTTP port")
    @Description("""
            Each row looks for the request's line in the request log (RequestLogFilter) after the start.
            The service is new and its database is empty.
            """)
    @TableTest("""
        Scenario                            | Method | Path                           | Request Body  | Status?
        Speaker form that fails validation  | POST   | /speakers                      | '{}'          | 400
        Unknown speaker                     | GET    | /speakers/0                    | ''            | 404
        Talk form that fails validation     | POST   | /talks                         | '{}'          | 400
        Unknown talk                        | GET    | /talks/0                       | ''            | 404
        Ranking for a speaker with no talks | GET    | /talks?speakerId=0&sort=rating | ''            | 200
        Rating for an unknown talk          | PUT    | /talks/0/ratings/1             | '{"score":3}' | 404
        """)
    void sendsEachWarmUpRequestThroughTheHttpPort(String method, String path, String requestBody, int status) {
        Pattern requestLogLine = Pattern.compile("RequestLogFilter\\s+: " + Pattern.quote(method + " " + path)
                + " -> " + status + " in \\d+ ms request=" + Pattern.quote(requestBody) + " response=");
        assertTrue(requestLogLine.matcher(startUpLog).find(), "no request log line " + requestLogLine);
    }

    @DisplayName("R2: Leaves every table as empty as it found it")
    @Description("""
            Each row counts the rows of a table after the "Warm-up done" line (R3).
            """)
    @TableTest("""
        Scenario       | Table                         | Rows After The Warm-Up?
        Empty database | {speaker, talk, rating, vote} | 0
        """)
    void leavesEveryTableAsEmptyAsItFoundIt(String table, int rowsAfterTheWarmUp) {
        assertTrue(WARM_UP_DONE.matcher(startUpLog).find(), "no 'Warm-up done' line within " + WARM_UP_LIMIT);
        assertEquals(rowsAfterTheWarmUp, jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class));
    }

    @DisplayName("R3: Writes the number of requests when the warm-up ends")
    @TableTest("""
        Scenario                   | Requests In The Log Line?
        Every warm-up request done | 6
        """)
    void writesTheNumberOfRequestsWhenTheWarmUpEnds(int requestsInTheLogLine) {
        Matcher done = WARM_UP_DONE.matcher(startUpLog);
        assertTrue(done.find(), "no 'Warm-up done' line within " + WARM_UP_LIMIT);
        assertEquals(requestsInTheLogLine, Integer.parseInt(done.group(1)));
    }

    private static String logUntilTheWarmUpIsDone(CapturedOutput output) throws InterruptedException {
        long deadline = System.nanoTime() + WARM_UP_LIMIT.toNanos();
        while (!WARM_UP_DONE.matcher(output.getOut()).find() && System.nanoTime() < deadline) {
            Thread.sleep(100);
        }
        return output.getOut();
    }
}
