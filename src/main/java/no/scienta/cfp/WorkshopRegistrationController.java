package no.scienta.cfp;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The HTTP API of workshop registration. Parses the request and maps each {@link RegistrationOutcome} to its status
 * code, as decided in workflow/specs/workshop-registration/spec.md.
 */
@RestController
public class WorkshopRegistrationController {

    private final WorkshopRegistrationService service;

    public WorkshopRegistrationController(WorkshopRegistrationService service) {
        this.service = service;
    }

    @PostMapping("/workshops/{id}/registrations")
    public ResponseEntity<Registration> register(@PathVariable long id, @RequestBody Map<String, Object> body) {
        RegistrationRequest request = RegistrationRequest.parse(body)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "a registration needs a name and a real email"));
        return created(service.register(id, request));
    }

    @GetMapping("/workshops/{id}/registrations")
    public List<Registration> attendees(@PathVariable long id) {
        if (service.attendees(id) instanceof AttendeesOutcome.Attendees attendees) {
            return attendees.registrations();
        }
        throw notAWorkshop();
    }

    /**
     * Unregisters when the body gives the registration's email (R9, R10). A wrong or missing email gets the same 404 as
     * an unknown registration, so the answer does not tell whether the registration exists.
     */
    @DeleteMapping("/workshops/{id}/registrations/{registrationId}")
    public ResponseEntity<Void> unregister(@PathVariable long id, @PathVariable long registrationId,
                                           @RequestBody Map<String, Object> body) {
        boolean unregistered = body.get("email") instanceof String email
                && service.unregister(id, registrationId, email) instanceof UnregistrationOutcome.Unregistered;
        if (unregistered) {
            return ResponseEntity.noContent().build();
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no such registration for this workshop");
    }

    private static ResponseEntity<Registration> created(RegistrationOutcome outcome) {
        if (outcome instanceof RegistrationOutcome.Registered registered) {
            return ResponseEntity.status(HttpStatus.CREATED).body(registered.registration());
        }
        throw refusal(outcome);
    }

    private static ResponseStatusException refusal(RegistrationOutcome outcome) {
        if (outcome instanceof RegistrationOutcome.NotAWorkshop) {
            return notAWorkshop();
        }
        if (outcome instanceof RegistrationOutcome.Closed closed) {
            return new ResponseStatusException(HttpStatus.CONFLICT,
                    "a workshop that is " + closed.status() + " takes no registrations");
        }
        if (outcome instanceof RegistrationOutcome.Full full) {
            return new ResponseStatusException(HttpStatus.CONFLICT, "all " + full.seats() + " seats are taken");
        }
        if (outcome instanceof RegistrationOutcome.AlreadyRegistered) {
            return new ResponseStatusException(HttpStatus.CONFLICT, "this email is already registered");
        }
        throw new IllegalArgumentException("unknown registration outcome " + outcome);
    }

    private static ResponseStatusException notAWorkshop() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "no workshop with this id");
    }
}
