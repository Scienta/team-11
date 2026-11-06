package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
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
            "ada lovelace@example.com,       false",
            "(64 × a)@example.com,                true",
            "(65 × a)@example.com,                false",
            "ada@(251 × e).com,                   true",
            "ada@(252 × e).com,                   false",
    })
    void acceptsOnlyARealEmailAddress(@ConvertWith(RepeatedCharacters.class) String email, boolean valid) {
        assertEquals(valid, EmailAddress.isValid(email));
    }

    /** Expands a run written as {@code (64 × a)} into the letter a written 64 times. */
    static class RepeatedCharacters extends SimpleArgumentConverter {
        private static final Pattern RUN = Pattern.compile("\\((\\d+) × (.)\\)");

        @Override
        protected Object convert(Object source, Class<?> targetType) {
            return RUN.matcher((String) source).replaceAll(run -> run.group(2).repeat(Integer.parseInt(run.group(1))));
        }
    }
}
