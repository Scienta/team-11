package no.scienta.cfp;

import java.util.EnumSet;
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
    private static final Set<TalkStatus> UNCHANGEABLE_STATUSES = EnumSet.of(TalkStatus.WITHDRAWN, TalkStatus.REJECTED);

    private final TalkDao talkDao;
    private final SpeakerDao speakerDao;
    private final CoSpeakerDao coSpeakerDao;
    private final TalkService conference;

    public CoSpeakerService(TalkDao talkDao, SpeakerDao speakerDao, CoSpeakerDao coSpeakerDao,
                            TalkService conference) {
        this.talkDao = talkDao;
        this.speakerDao = speakerDao;
        this.coSpeakerDao = coSpeakerDao;
        this.conference = conference;
    }

    @Transactional
    public TalkResponse add(long talkId, long speakerId) {
        Talk talk = lockedTalk(talkId);
        requireRegistered(speakerId);
        requireChangeable(talk);
        if (talk.speakerId() == speakerId) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "the main speaker can't be a co-speaker");
        }
        if (!talk.coSpeakerIds().contains(speakerId)) {
            if (talk.coSpeakerIds().size() >= MAX_CO_SPEAKERS) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "a talk has at most " + MAX_CO_SPEAKERS + " co-speakers");
            }
            coSpeakerDao.add(talkId, speakerId);
        }
        return conference.getTalk(talkId);
    }

    @Transactional
    public TalkResponse remove(long talkId, long speakerId) {
        Talk talk = lockedTalk(talkId);
        requireRegistered(speakerId);
        requireChangeable(talk);
        if (talk.speakerId() == speakerId) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "the main speaker can't be removed");
        }
        coSpeakerDao.remove(talkId, speakerId);
        return conference.getTalk(talkId);
    }

    /** The talk, locked until the transaction ends. */
    private Talk lockedTalk(long talkId) {
        return talkDao.findByIdForUpdate(talkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "talk not found"));
    }

    private void requireRegistered(long speakerId) {
        if (!speakerDao.exists(speakerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "speaker not found");
        }
    }

    private static void requireChangeable(Talk talk) {
        if (UNCHANGEABLE_STATUSES.contains(TalkStatus.valueOf(talk.status()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "a withdrawn or rejected talk can't be changed");
        }
    }
}
