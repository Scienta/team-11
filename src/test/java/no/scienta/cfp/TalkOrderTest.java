package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

/**
 * The order of the talk list, read from the {@code sort} parameter of {@code GET /talks} (Ratings R5, R6).
 */
class TalkOrderTest {

    @DisplayName("Reads the order of the talk list from the sort parameter")
    @Description("""
            A blank Sort sends no parameter. A blank Order means that the value is not an order:
            GET /talks answers 400 for it.
            """)
    @TableTest("""
        Scenario                 | Sort                | Order?
        No sort                  |                     | OLDEST_FIRST
        By rating                | rating              | BY_RATING
        Another word or spelling | {bogus, RATING, ''} |
        """)
    void readsTheOrderFromTheSortParameter(String sort, TalkOrder order) {
        assertEquals(Optional.ofNullable(order), TalkOrder.parse(sort));
    }
}
