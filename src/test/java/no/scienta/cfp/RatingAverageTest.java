package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RatingAverageTest {

    @Test
    void roundsToOneDecimal() {
        assertEquals(4.2, RatingAverage.roundToOneDecimal(4.25));
        assertEquals(3.6, RatingAverage.roundToOneDecimal(3.6666));
        assertEquals(4.5, RatingAverage.roundToOneDecimal(4.5));
    }
}
