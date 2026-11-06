package no.scienta.cfp;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.stereotype.Component;

/**
 * Refuses a JSON text that holds a NUL character, which the database cannot store (security review, 2026-11-06).
 * Spring adds the module to its JSON mapper, so such a request body gets 400 like any other unreadable body.
 */
@Component
public class TextWithoutNulModule extends SimpleModule {

    public TextWithoutNulModule() {
        super("text-without-nul");
        addDeserializer(String.class, new TextWithoutNul());
    }

    /** Reads a JSON string as usual, and fails when it holds a NUL character. */
    static final class TextWithoutNul extends StringDeserializer {

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String text = super.deserialize(parser, context);
            if (text != null && text.indexOf('\0') >= 0) {
                return (String) context.handleWeirdStringValue(String.class, text, "a text cannot hold a NUL character");
            }
            return text;
        }
    }
}
