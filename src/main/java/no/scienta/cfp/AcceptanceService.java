package no.scienta.cfp;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The committee's decisions on a talk and the speaker's withdrawal (Acceptance R1–R6). A refused change leaves the
 * talk as it was. Each change runs in one transaction, so it holds one database connection.
 */
@Service
public class AcceptanceService {

    private final ConferenceService conference;
    private final TalkDao talkDao;
    private final TalkStatusDao statusDao;

    public AcceptanceService(ConferenceService conference, TalkDao talkDao, TalkStatusDao statusDao) {
        this.conference = conference;
        this.talkDao = talkDao;
        this.statusDao = statusDao;
    }

    @Transactional
    public StatusChange decide(long talkId, Decision decision) {
        return findTalk(talkId)
                .map(talk -> moveTo(talk, decision.status()))
                .orElseGet(StatusChange.NotFound::new);
    }

    @Transactional
    public StatusChange withdraw(long talkId, Withdrawal withdrawal) {
        return findTalk(talkId)
                .map(talk -> withdrawFor(talk, withdrawal.speakerId()))
                .orElseGet(StatusChange.NotFound::new);
    }

    private StatusChange withdrawFor(TalkDto talk, long speakerId) {
        return talk.speakerId == speakerId ? moveTo(talk, TalkStatus.WITHDRAWN) : new StatusChange.NotMainSpeaker();
    }

    /**
     * Moves the talk if the rules allow it. A refusal is explained from the status that the talk had when it was
     * read; if another request changed the talk in between, the reason can be out of date, but the talk is not
     * changed either way.
     */
    private StatusChange moveTo(TalkDto talk, TalkStatus next) {
        if (statusDao.moveTo(talk.id, next)) {
            return new StatusChange.Moved(conference.getTalk(talk.id));
        }
        TalkStatus current = talk.status;
        return current.canMoveTo(next) ? new StatusChange.NeedsReviewers(next) : new StatusChange.NotAllowed(current, next);
    }

    /** The talk without its ratings. */
    private Optional<TalkDto> findTalk(long talkId) {
        return Optional.ofNullable(talkDao.findById(talkId));
    }
}
