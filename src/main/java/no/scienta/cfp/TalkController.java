package no.scienta.cfp;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TalkController {

    private final TalkService service;
    private final RatingService ratings;

    public TalkController(TalkService service, RatingService ratings) {
        this.service = service;
        this.ratings = ratings;
    }

    @PostMapping("/talks")
    @ResponseStatus(HttpStatus.CREATED)
    public TalkResponse submit(@RequestBody TalkForm form) {
        return service.submitTalk(form);
    }

    @GetMapping("/talks/{id}")
    public TalkResponse get(@PathVariable Long id) {
        return service.getTalk(id);
    }

    @GetMapping("/talks")
    public List<TalkResponse> list(@RequestParam(required = false) String sort,
                              @RequestParam(required = false) Long speakerId,
                              @RequestParam(defaultValue = "true") String includeWithdrawn) {
        return switch (TalkListQuery.parse(sort, speakerId, includeWithdrawn)) {
            case TalkListQuery.Valid valid -> service.listTalks(valid.query());
            case TalkListQuery.Invalid invalid -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, invalid.reason());
        };
    }

    @PutMapping("/talks/{id}/ratings/{reviewerId}")
    public Map<String, Object> rate(@PathVariable Long id, @PathVariable Long reviewerId,
                                    @RequestBody Map<String, Object> body) {
        return ratings.rate(id, reviewerId, body);
    }
}
