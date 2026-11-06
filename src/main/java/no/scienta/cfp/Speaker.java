package no.scienta.cfp;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A registered speaker, as stored and as the API returns it.
 */
@JsonPropertyOrder({"id", "name", "email", "bio"})
public record Speaker(long id, String name, String email, String bio) {
}
