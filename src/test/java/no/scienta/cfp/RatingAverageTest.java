package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RatingAverageTest {

    @Test
    void roundsToOneDecimalWithHalvesUp() {
        assertEquals(4.3, RatingAverage.roundToOneDecimal(4.25));
        assertEquals(3.7, RatingAverage.roundToOneDecimal(3.6666));
        assertEquals(4.5, RatingAverage.roundToOneDecimal(4.5));
    }
}
