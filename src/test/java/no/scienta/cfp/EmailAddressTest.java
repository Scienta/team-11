package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EmailAddressTest {

    // Speakers R3; the rows are the example table in workflow/specs/speakers/spec.md
    @ParameterizedTest(name = "{0} is an email: {1}")
    @CsvSource({
            "ada@example.com,                     true",
            "ada.lovelace+cfp@mail.example.co.uk, true",
            "not-an-email,                        false",
            "ada@example,                         false",
            "@example.com,                        false",
            "ada@.com,                            false",
            "ada@example.,                        false",
            "ada@example..com,                    false",
            "ada@@example.com,                    false",
            "ada lovelace@example.com,            false",
            "' ada@example.com',                  false",
            "ada\u00a0lovelace@example.com,       false",
    })
    void acceptsOnlyARealEmailAddress(String email, boolean valid) {
        assertEquals(valid, EmailAddress.isValid(email));
    }
}
