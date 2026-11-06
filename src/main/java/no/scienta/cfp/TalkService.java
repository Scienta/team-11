package no.scienta.cfp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Submitting, looking up and listing talks.
 */
@Service
public class TalkService {

    private static final int MAX_TALKS_PER_SPEAKER = 3;

    /** The order the 400 message has always listed the formats in; clients may compare the message. */
    private static final List<TalkFormat> FORMATS_IN_ERROR_MESSAGE = List.of(
            TalkFormat.WORKSHOP, TalkFormat.PRESENTATION, TalkFormat.LIGHTNING_20, TalkFormat.LIGHTNING_10);

    private final SpeakerDao speakerDao;
    private final TalkDao talkDao;
    private final RatingService ratings;
    private final NotificationService notifications;

    public TalkService(SpeakerDao speakerDao, TalkDao talkDao, RatingService ratings,
                             NotificationService notifications) {
        this.speakerDao = speakerDao;
        this.talkDao = talkDao;
        this.ratings = ratings;
        this.notifications = notifications;
    }

    @Transactional
    public TalkDto submitTalk(TalkForm form) {
        if (form.title == null || form.title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        TalkFormat format = constantNamed(TalkFormat.class, form.format).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "format must be one of " + FORMATS_IN_ERROR_MESSAGE));
        if (constantNamed(TalkLevel.class, form.level).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown level");
        }
        if (form.speakerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "speakerId is required");
        }
        if (!speakerDao.exists(form.speakerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "speaker " + form.speakerId + " does not exist");
        }

        Long id = checkSubmissionLimit(form, format.durationMinutes());
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "a speaker can submit at most " + MAX_TALKS_PER_SPEAKER + " talks");
        }
        notifications.talkSubmitted(speakerDao.findById(form.speakerId).orElseThrow().email(), form.title);
        return getTalk(id);
    }

    /** The constant with exactly this name, in capitals as declared; none for null or any other name. */
    private static <E extends Enum<E>> Optional<E> constantNamed(Class<E> type, String name) {
        return Arrays.stream(type.getEnumConstants()).filter(constant -> constant.name().equals(name)).findFirst();
    }

    private Long checkSubmissionLimit(TalkForm form, int duration) {
        return talkDao.insertIfBelow(MAX_TALKS_PER_SPEAKER, form.title, form.format, duration, form.level, form.speakerId,
                System.currentTimeMillis());
    }

    public TalkDto getTalk(long id) {
        TalkDto talk = talkDao.findById(id);
        if (talk == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "talk not found");
        }
        ratings.addRatings(talk);
        return talk;
    }

    public List<TalkDto> getTalksForSpeaker(long speakerId) {
        if (speakerDao.findById(speakerId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "speaker not found");
        }
        List<TalkDto> talks = talkDao.findBySpeaker(speakerId);
        for (TalkDto t : talks) {
            ratings.addRatings(t);
        }
        return talks;
    }

    /** The talk list without the withdrawn talks when {@code includeWithdrawn} is false (Acceptance R9). */
    public List<TalkDto> listTalks(String sort, Long speakerId, boolean includeWithdrawn) {
        return listTalks(sort, speakerId).stream()
                .filter(talk -> includeWithdrawn || !isWithdrawn(talk))
                .collect(Collectors.toList());
    }

    /** The talks in the order of Ratings R5 or R6, with the withdrawn talks after the others (Acceptance R9). */
    public List<TalkDto> listTalks(String sort, Long speakerId) {
        Map<Boolean, List<TalkDto>> byWithdrawn = orderedTalks(sort, speakerId).stream()
                .collect(Collectors.partitioningBy(TalkService::isWithdrawn));
        List<TalkDto> talks = new ArrayList<>(byWithdrawn.get(false));
        talks.addAll(byWithdrawn.get(true));
        return talks;
    }

    private static boolean isWithdrawn(TalkDto talk) {
        return TalkStatus.WITHDRAWN.name().equals(talk.status);
    }

    private List<TalkDto> orderedTalks(String sort, Long speakerId) {
        if (sort != null && !sort.equals("rating")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sort must be 'rating'");
        }
        List<TalkDto> talks = speakerId == null ? talkDao.findAll() : talkDao.findBySpeaker(speakerId);
        for (TalkDto t : talks) {
            ratings.addRatings(t);
        }
        if ("rating".equals(sort)) {
            List<TalkDto> rated = new ArrayList<>();
            List<TalkDto> unrated = new ArrayList<>();
            for (TalkDto t : talks) {
                if (t.ratingCount > 0) {
                    rated.add(t);
                } else {
                    unrated.add(t);
                }
            }
            rated.sort((a, b) -> {
                int c = Double.compare(b.averageRating, a.averageRating);
                if (c != 0) {
                    return c;
                }
                c = Integer.compare(b.ratingCount, a.ratingCount);
                if (c != 0) {
                    return c;
                }
                return Long.compare(a.id, b.id);
            });
            rated.addAll(unrated);
            return rated;
        }
        return talks;
    }
}
