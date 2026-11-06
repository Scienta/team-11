package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.TableTest;
import org.tabletest.junit.TypeConverter;

/**
 * Which warm-up outcomes give a warning in the log (workflow/specs/warm-up/spec.md, R4).
 */
@DisplayName("Warm-up outcomes")
public class WarmUpOutcomeTest {

    @DisplayName("R4: Warns about an unexpected status or a failure")
    @TableTest("""
        Scenario        | Outcome                                                    | Warning?
        Expected status | [request: POST /speakers, expects: 400, answered: 400]     |
        Other status    | [request: POST /speakers, expects: 400, answered: 500]     | Warm-up POST /speakers answered 500, expected 400
        No answer       | [request: POST /speakers, expects: 400, failed: timed out] | Warm-up POST /speakers failed: timed out
        """)
    void warnsAboutAnUnexpectedStatusOrAFailure(WarmUpOutcome outcome, String warning) {
        assertEquals(warning, outcome.warning().orElse(null));
    }

    @TypeConverter
    public static WarmUpOutcome parseWarmUpOutcome(Map<String, String> fields) {
        String[] methodAndPath = fields.get("request").split(" ");
        WarmUpRequest request = new WarmUpRequest(methodAndPath[0], methodAndPath[1], "",
                Integer.parseInt(fields.get("expects")));
        return fields.containsKey("failed")
                ? new WarmUpOutcome.Failed(request, fields.get("failed"))
                : new WarmUpOutcome.Answered(request, Integer.parseInt(fields.get("answered")));
    }
}
