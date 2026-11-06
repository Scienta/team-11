package no.scienta.cfp;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Adds and removes a talk's co-speakers. Each change locks the talk row first, so simultaneous changes to one talk
 * see each other and the limit of 2 holds.
 */
@Service
public class CoSpeakerService {

    private static final int MAX_CO_SPEAKERS = 2;
    private static final Set<String> UNCHANGEABLE_STATUSES = Set.of("WITHDRAWN", "REJECTED");

    private final TalkDao talkDao;
    private final SpeakerDao speakerDao;
    private final CoSpeakerDao coSpeakerDao;
    private final ConferenceService conference;

    public CoSpeakerService(TalkDao talkDao, SpeakerDao speakerDao, CoSpeakerDao coSpeakerDao,
                            ConferenceService conference) {
        this.talkDao = talkDao;
        this.speakerDao = speakerDao;
        this.coSpeakerDao = coSpeakerDao;
        this.conference = conference;
    }

    @Transactional
    public TalkDto add(long talkId, long speakerId) {
        TalkDto talk = changeableTalk(talkId, speakerId);
        if (talk.speakerId == speakerId) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "the main speaker can't be a co-speaker");
        }
        if (!talk.coSpeakerIds.contains(speakerId)) {
            if (talk.coSpeakerIds.size() >= MAX_CO_SPEAKERS) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "a talk has at most " + MAX_CO_SPEAKERS + " co-speakers");
            }
            coSpeakerDao.add(talkId, speakerId);
        }
        return conference.getTalk(talkId);
    }

    @Transactional
    public TalkDto remove(long talkId, long speakerId) {
        TalkDto talk = changeableTalk(talkId, speakerId);
        if (talk.speakerId == speakerId) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "the main speaker can't be removed");
        }
        coSpeakerDao.remove(talkId, speakerId);
        return conference.getTalk(talkId);
    }

    /** The talk, locked for this transaction, when both ids are known and the talk may still change. */
    private TalkDto changeableTalk(long talkId, long speakerId) {
        TalkDto talk = talkDao.findByIdForUpdate(talkId);
        if (talk == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "talk not found");
        }
        if (!speakerDao.exists(speakerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "speaker not found");
        }
        if (UNCHANGEABLE_STATUSES.contains(talk.status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "a withdrawn or rejected talk can't be changed");
        }
        return talk;
    }
}
