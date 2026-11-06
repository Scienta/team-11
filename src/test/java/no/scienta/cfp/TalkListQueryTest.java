package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

/**
 * The query of {@code GET /talks}, read from its parameters: the order, one speaker or all, and whether withdrawn talks
 * are in the list (Ratings R5, R6, Acceptance R9).
 */
class TalkListQueryTest {

    @DisplayName("Reads the talk list query from the parameters")
    @Description("""
            A blank cell sends no parameter. Include Withdrawn is the value that the controller passes
            on: "true" when the request has no such parameter.
            """)
    @TableTest("""
        Scenario                  | Sort   | Speaker | Include Withdrawn | Order?       | One Speaker? | Withdrawn Included?
        No parameters             |        |         | true              | OLDEST_FIRST |              | true
        By rating for one speaker | rating | 7       | false             | BY_RATING    | 7            | false
        """)
    void readsTheQueryFromTheParameters(String sort, Long speaker, String includeWithdrawn, TalkOrder order,
                                        Long oneSpeaker, boolean withdrawnIncluded) {
        assertEquals(new TalkListQuery.Valid(new TalkListQuery(order, Optional.ofNullable(oneSpeaker), withdrawnIncluded)),
                TalkListQuery.parse(sort, speaker, includeWithdrawn));
    }

    @DisplayName("Refuses a parameter that is not a value of the query")
    @Description("""
            The reason is the message of the 400 answer. includeWithdrawn is checked before sort, as
            before.
            """)
    @TableTest("""
        Scenario                      | Sort  | Include Withdrawn | Refusal?
        Unknown sort                  | bogus | true              | sort must be 'rating'
        includeWithdrawn not a choice |       | yes               | includeWithdrawn must be true or false
        Both wrong                    | bogus | yes               | includeWithdrawn must be true or false
        """)
    void refusesAParameterThatIsNotAValue(String sort, String includeWithdrawn, String refusal) {
        assertEquals(new TalkListQuery.Invalid(refusal), TalkListQuery.parse(sort, null, includeWithdrawn));
    }
}
