package no.scienta.cfp;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The HTTP API of co-speakers. Maps each {@link CoSpeakerChange} to its status code, as decided in
 * workflow/specs/co-speakers/spec.md.
 */
@RestController
@RequestMapping("/talks/{id}/co-speakers/{speakerId}")
public class CoSpeakerController {

    private final CoSpeakerService service;

    public CoSpeakerController(CoSpeakerService service) {
        this.service = service;
    }

    @PutMapping
    public TalkResponse add(@PathVariable Long id, @PathVariable Long speakerId,
                       @RequestParam Optional<Long> actingSpeakerId) {
        return answer(service.add(id, speakerId, actingSpeakerId), "the main speaker can't be a co-speaker");
    }

    @DeleteMapping
    public TalkResponse remove(@PathVariable Long id, @PathVariable Long speakerId,
                          @RequestParam Optional<Long> actingSpeakerId) {
        return answer(service.remove(id, speakerId, actingSpeakerId), "the main speaker can't be removed");
    }

    /** The changed talk, or the refusal as an error; the message for the main speaker depends on the request. */
    private static TalkResponse answer(CoSpeakerChange change, String mainSpeakerMessage) {
        if (change instanceof CoSpeakerChange.Refusal refused) {
            throw refusal(refused, mainSpeakerMessage);
        }
        return ((CoSpeakerChange.Changed) change).talk();
    }

    private static ResponseStatusException refusal(CoSpeakerChange.Refusal change, String mainSpeakerMessage) {
        if (change instanceof CoSpeakerChange.TalkNotFound) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "talk not found");
        }
        if (change instanceof CoSpeakerChange.NotMainSpeaker) {
            return new ResponseStatusException(HttpStatus.FORBIDDEN, "only the main speaker can change the co-speakers");
        }
        if (change instanceof CoSpeakerChange.SpeakerNotFound) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "speaker not found");
        }
        if (change instanceof CoSpeakerChange.Closed) {
            return new ResponseStatusException(HttpStatus.CONFLICT, "a withdrawn or rejected talk can't be changed");
        }
        if (change instanceof CoSpeakerChange.MainSpeaker) {
            return new ResponseStatusException(HttpStatus.CONFLICT, mainSpeakerMessage);
        }
        if (change instanceof CoSpeakerChange.Full full) {
            return new ResponseStatusException(HttpStatus.CONFLICT,
                    "a talk has at most " + full.coSpeakers() + " co-speakers");
        }
        throw new IllegalArgumentException("unknown co-speaker refusal " + change);
    }
}
