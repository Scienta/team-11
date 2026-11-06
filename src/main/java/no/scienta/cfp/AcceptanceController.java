package no.scienta.cfp;

import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP API of the acceptance state machine: the committee's decision and the speaker's withdrawal.
 */
@RestController
public class AcceptanceController {

    private final AcceptanceService service;

    public AcceptanceController(AcceptanceService service) {
        this.service = service;
    }

    @PutMapping("/talks/{id}/status")
    public TalkResponse decide(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return service.decide(id, body);
    }

    @PostMapping("/talks/{id}/withdraw")
    public TalkResponse withdraw(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return service.withdraw(id, body);
    }
}
