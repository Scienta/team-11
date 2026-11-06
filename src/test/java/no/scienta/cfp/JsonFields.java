package no.scienta.cfp;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The field names of a JSON object, in the order the response has them.
 */
final class JsonFields {

    private JsonFields() {
    }

    static List<String> namesOf(JsonNode object) {
        List<String> names = new ArrayList<>();
        object.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
