package no.scienta.cfp;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * The committee's decisions on a talk and the speaker's withdrawal (Acceptance R1–R6). A refused change leaves the
 * talk as it was.
 */
@Service
public class AcceptanceService {

    private final TalkService conference;
    private final TalkStatusDao statusDao;

    public AcceptanceService(TalkService conference, TalkStatusDao statusDao) {
        this.conference = conference;
        this.statusDao = statusDao;
    }

    public TalkDto decide(long talkId, Map<String, Object> body) {
        TalkStatus decision = TalkStatus.parseDecision(body.get("status"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "status must be ACCEPTED, REJECTED or WAITLISTED"));
        conference.getTalk(talkId);
        return moveTo(talkId, decision);
    }

    public TalkDto withdraw(long talkId, Map<String, Object> body) {
        long speakerId = speakerIdOf(body.get("speakerId"));
        TalkDto talk = conference.getTalk(talkId);
        if (talk.speakerId != speakerId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only the main speaker can withdraw the talk");
        }
        return moveTo(talkId, TalkStatus.WITHDRAWN);
    }

    private TalkDto moveTo(long talkId, TalkStatus next) {
        if (!statusDao.moveTo(talkId, next)) {
            TalkStatus current = TalkStatus.valueOf(conference.getTalk(talkId).status);
            throw new ResponseStatusException(HttpStatus.CONFLICT, refusal(current, next));
        }
        return conference.getTalk(talkId);
    }

    private static String refusal(TalkStatus current, TalkStatus next) {
        return current.canMoveTo(next)
                ? "a talk needs ratings from " + next.reviewersNeeded() + " different reviewers to become " + next
                : "a talk that is " + current + " cannot become " + next;
    }

    private static long speakerIdOf(Object value) {
        if (value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "speakerId must be a whole number");
    }
}
