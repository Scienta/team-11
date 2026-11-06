package no.scienta.cfp;

import java.util.Map;
import java.util.Optional;

/**
 * An attendee's request to register for a workshop: a real name and a real email (workshop-registration R1). Only
 * {@link #parse} creates one, so every instance is valid.
 */
public record RegistrationRequest(String name, String email) {

    /** The request from a JSON body, or empty when the name or the email is missing or not real. */
    public static Optional<RegistrationRequest> parse(Map<String, Object> body) {
        Object name = body.get("name");
        Object email = body.get("email");
        if (name instanceof String realName && !realName.isBlank()
                && email instanceof String address && Utils.isValidEmail(address)) {
            return Optional.of(new RegistrationRequest(realName, address));
        }
        return Optional.empty();
    }
}
