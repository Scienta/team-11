package no.scienta.cfp;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/talks/{id}/co-speakers/{speakerId}")
public class CoSpeakerController {

    private final CoSpeakerService service;

    public CoSpeakerController(CoSpeakerService service) {
        this.service = service;
    }

    @PutMapping
    public TalkResponse add(@PathVariable Long id, @PathVariable Long speakerId) {
        return service.add(id, speakerId);
    }

    @DeleteMapping
    public TalkResponse remove(@PathVariable Long id, @PathVariable Long speakerId) {
        return service.remove(id, speakerId);
    }
}
