package no.scienta.cfp;

import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * The field names of a JSON object, in the order the response has them.
 */
final class JsonFields {

    private JsonFields() {
    }

    static List<String> namesOf(JsonNode object) {
        return List.copyOf(object.propertyNames());
    }
}
