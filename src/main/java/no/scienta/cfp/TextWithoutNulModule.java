package no.scienta.cfp;

import org.springframework.stereotype.Component;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import tools.jackson.databind.module.SimpleModule;

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
        public String deserialize(JsonParser parser, DeserializationContext context) {
            String text = super.deserialize(parser, context);
            if (text != null && text.indexOf('\0') >= 0) {
                return (String) context.handleWeirdStringValue(String.class, text, "a text cannot hold a NUL character");
            }
            return text;
        }
    }
}
