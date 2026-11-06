package no.scienta.cfp;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The HTTP API of the acceptance state machine: the committee's decision and the speaker's withdrawal. Parses the
 * request bodies and turns each refused status change into its HTTP status.
 */
@RestController
public class AcceptanceController {

    private final AcceptanceService service;

    public AcceptanceController(AcceptanceService service) {
        this.service = service;
    }

    @PutMapping("/talks/{id}/status")
    public TalkDto decide(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Decision decision = Decision.parse(body).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "status must be ACCEPTED, REJECTED or WAITLISTED"));
        return answer(service.decide(id, decision));
    }

    @PostMapping("/talks/{id}/withdraw")
    public TalkDto withdraw(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Withdrawal withdrawal = Withdrawal.parse(body).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "speakerId must be a whole number"));
        return answer(service.withdraw(id, withdrawal));
    }

    private static TalkDto answer(StatusChange change) {
        if (change instanceof StatusChange.Moved moved) {
            return moved.talk();
        }
        throw refusal(change);
    }

    private static ResponseStatusException refusal(StatusChange change) {
        if (change instanceof StatusChange.NotFound) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "talk not found");
        }
        if (change instanceof StatusChange.NotMainSpeaker) {
            return new ResponseStatusException(HttpStatus.FORBIDDEN, "only the main speaker can withdraw the talk");
        }
        if (change instanceof StatusChange.NotAllowed notAllowed) {
            return new ResponseStatusException(HttpStatus.CONFLICT,
                    "a talk that is " + notAllowed.current() + " cannot become " + notAllowed.next());
        }
        if (change instanceof StatusChange.NeedsReviewers needsReviewers) {
            TalkStatus next = needsReviewers.next();
            return new ResponseStatusException(HttpStatus.CONFLICT,
                    "a talk needs ratings from " + next.reviewersNeeded() + " different reviewers to become " + next);
        }
        throw new IllegalArgumentException("unknown status change " + change);
    }
}
