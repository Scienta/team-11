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
        return service.attendees(id).orElseThrow(WorkshopRegistrationController::notAWorkshop);
    }

    @DeleteMapping("/workshops/{id}/registrations/{registrationId}")
    public ResponseEntity<Void> unregister(@PathVariable long id, @PathVariable long registrationId) {
        if (service.unregister(id, registrationId) instanceof UnregistrationOutcome.Unregistered) {
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
        return notAWorkshop();
    }

    private static ResponseStatusException notAWorkshop() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "no workshop with this id");
    }
}
