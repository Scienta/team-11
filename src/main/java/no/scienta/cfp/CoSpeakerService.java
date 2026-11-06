package no.scienta.cfp;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds and removes a talk's co-speakers, and returns the expected refusals as a {@link CoSpeakerChange}. Each change
 * locks the talk row first, so simultaneous changes to one talk see each other and the limit of 2 holds.
 */
@Service
public class CoSpeakerService {

    private static final int MAX_CO_SPEAKERS = 2;

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
    public CoSpeakerChange add(long talkId, long speakerId, Optional<Long> actingSpeakerId) {
        return lockedTalk(talkId)
                .map(talk -> refusal(talk, speakerId, actingSpeakerId, new CoSpeakerChange.MainSpeakerAsCoSpeaker())
                        .orElseGet(() -> addTo(talk, speakerId)))
                .orElseGet(CoSpeakerChange.TalkNotFound::new);
    }

    @Transactional
    public CoSpeakerChange remove(long talkId, long speakerId, Optional<Long> actingSpeakerId) {
        return lockedTalk(talkId)
                .map(talk -> refusal(talk, speakerId, actingSpeakerId, new CoSpeakerChange.MainSpeaker())
                        .orElseGet(() -> removeFrom(talk, speakerId)))
                .orElseGet(CoSpeakerChange.TalkNotFound::new);
    }

    /**
     * Adds the speaker last, unless they already are a co-speaker (R5), the talk is full (R2), or the speaker is at the
     * limit of talks (submission-cap-cr R5). The speaker row is locked after the talk row, so a submission and this
     * place for the same speaker take turns (submission-cap-cr R6).
     */
    private CoSpeakerChange addTo(TalkDto talk, long speakerId) {
        if (talk.coSpeakerIds.contains(speakerId)) {
            return changed(talk);
        }
        if (talk.coSpeakerIds.size() >= MAX_CO_SPEAKERS) {
            return new CoSpeakerChange.Full(MAX_CO_SPEAKERS);
        }
        if (talkDao.lockSpeakerAndCountTalksTowardLimit(speakerId) >= ConferenceService.MAX_TALKS_PER_SPEAKER) {
            return new CoSpeakerChange.SpeakerAtLimit(ConferenceService.MAX_TALKS_PER_SPEAKER);
        }
        coSpeakerDao.add(talk.id, speakerId);
        return changed(talk);
    }

    /** Removes the speaker, unless they are not a co-speaker of the talk, or no longer are one. */
    private CoSpeakerChange removeFrom(TalkDto talk, long speakerId) {
        if (!talk.coSpeakerIds.contains(speakerId)) {
            return new CoSpeakerChange.NotCoSpeaker();
        }
        coSpeakerDao.remove(talk.id, speakerId);
        return changed(talk);
    }

    /**
     * The refusals that adding and removing share, in the order they are checked. Who acts is checked only when the
     * request says (R12). The main speaker as the target gets the refusal of the request: R3 for adding, R8 for
     * removing.
     */
    private Optional<CoSpeakerChange> refusal(TalkDto talk, long speakerId, Optional<Long> actingSpeakerId,
                                              CoSpeakerChange.Refusal mainSpeakerRefusal) {
        if (actingSpeakerId.filter(acting -> !acting.equals(talk.speakerId)).isPresent()) {
            return Optional.of(new CoSpeakerChange.NotMainSpeaker());
        }
        if (!speakerDao.exists(speakerId)) {
            return Optional.of(new CoSpeakerChange.SpeakerNotFound());
        }
        if (!talk.status.isOpenForCoSpeakerChanges()) {
            return Optional.of(new CoSpeakerChange.Closed());
        }
        if (talk.speakerId == speakerId) {
            return Optional.of(mainSpeakerRefusal);
        }
        return Optional.empty();
    }

    private CoSpeakerChange changed(TalkDto talk) {
        return new CoSpeakerChange.Changed(conference.getTalk(talk.id));
    }

    /** The talk, locked until the transaction ends. */
    private Optional<TalkDto> lockedTalk(long talkId) {
        return Optional.ofNullable(talkDao.findByIdForUpdate(talkId));
    }
}
