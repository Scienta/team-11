package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CsvFieldTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(value = {
            "Plain title           | Plain title",
            "Semi; colon           | \"Semi; colon\"",
            "Say \"hi\"            | \"Say 'hi'\"",
    }, delimiter = '|')
    void quotesATextThatHoldsASemicolonOrAQuote(String value, String field) {
        assertEquals(field, CsvField.text(value));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(value = {
            "WORKSHOP     | Workshop",
            "LIGHTNING 10 | Lightning 10",
            "'   '        | '   '",
    }, delimiter = '|')
    void capitalizesTheFirstLetterOnly(String value, String field) {
        assertEquals(field, CsvField.capitalized(value));
    }
}
