package no.scienta.cfp;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The HTTP API of programme scheduling. Parses the place and the body, and maps each {@link ScheduleOutcome} to its
 * status code, as decided in workflow/specs/scheduling/spec.md.
 */
@RestController
@RequestMapping("/programme/{room}/{slot}")
public class ProgrammeController {

    private final ProgrammeService service;

    public ProgrammeController(ProgrammeService service) {
        this.service = service;
    }

    @PutMapping
    public ProgrammeEntry schedule(@PathVariable String room, @PathVariable String slot,
                                   @RequestBody Map<String, Object> body) {
        Place place = place(room, slot);
        SchedulingRequest request = SchedulingRequest.parse(body)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "talkId must be an integer"));
        return scheduled(service.schedule(place, request));
    }

    @GetMapping
    public ProgrammeEntry entry(@PathVariable String room, @PathVariable String slot) {
        return service.entryAt(place(room, slot)).orElseThrow(ProgrammeController::nothingThere);
    }

    @DeleteMapping
    public ResponseEntity<Void> takeOut(@PathVariable String room, @PathVariable String slot) {
        if (service.takeOut(place(room, slot))) {
            return ResponseEntity.noContent().build();
        }
        throw nothingThere();
    }

    private static Place place(String room, String slot) {
        return Place.parse(room, slot).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "the room must be one of R1, R2, R3 and the slot one of S1-S5"));
    }

    private static ProgrammeEntry scheduled(ScheduleOutcome outcome) {
        if (outcome instanceof ScheduleOutcome.Scheduled scheduled) {
            return scheduled.entry();
        }
        throw refusal((ScheduleOutcome.Refusal) outcome);
    }

    private static ResponseStatusException refusal(ScheduleOutcome.Refusal outcome) {
        if (outcome instanceof ScheduleOutcome.NoSuchTalk) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, "no talk with this talkId");
        }
        if (outcome instanceof ScheduleOutcome.NotAccepted notAccepted) {
            return conflict("a talk that is " + notAccepted.status() + " does not go on the programme");
        }
        if (outcome instanceof ScheduleOutcome.DoesNotFit doesNotFit) {
            return conflict("a " + doesNotFit.format() + " does not fit slot " + doesNotFit.slot());
        }
        if (outcome instanceof ScheduleOutcome.PlaceTaken) {
            return conflict("another talk is in this room and slot");
        }
        if (outcome instanceof ScheduleOutcome.AlreadyScheduled) {
            return conflict("the talk is on the programme already; take it out first");
        }
        if (outcome instanceof ScheduleOutcome.SpeakerBusy) {
            return conflict("a speaker of the talk speaks in another room in this slot");
        }
        throw new IllegalArgumentException("unknown scheduling refusal " + outcome);
    }

    private static ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }

    private static ResponseStatusException nothingThere() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "no talk in this room and slot");
    }
}
