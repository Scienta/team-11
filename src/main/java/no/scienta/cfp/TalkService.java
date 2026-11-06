package no.scienta.cfp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

    private static final int MAX_TITLE_CHARACTERS = 200;

    /** The order the 400 message has always listed the formats in; clients may compare the message. */
    private static final List<TalkFormat> FORMATS_IN_ERROR_MESSAGE = List.of(
            TalkFormat.WORKSHOP, TalkFormat.PRESENTATION, TalkFormat.LIGHTNING_20, TalkFormat.LIGHTNING_10);

    /** Ratings R6: the best shown average first, then the most ratings, then the oldest. */
    private static final Comparator<TalkResponse> RANKING = Comparator
            .comparing(TalkResponse::averageRating, Comparator.reverseOrder())
            .thenComparing(TalkResponse::ratingCount, Comparator.reverseOrder())
            .thenComparing(TalkResponse::id);

    private final SpeakerDao speakerDao;
    private final TalkDao talkDao;
    private final RatingService ratings;
    private final NotificationService notifications;
    private final Timestamps timestamps;

    public TalkService(SpeakerDao speakerDao, TalkDao talkDao, RatingService ratings,
                             NotificationService notifications, Timestamps timestamps) {
        this.speakerDao = speakerDao;
        this.talkDao = talkDao;
        this.ratings = ratings;
        this.notifications = notifications;
        this.timestamps = timestamps;
    }

    @Transactional
    public TalkResponse submitTalk(TalkForm form) {
        if (form.title == null || form.title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        if (form.title.codePointCount(0, form.title.length()) > MAX_TITLE_CHARACTERS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "title is longer than " + MAX_TITLE_CHARACTERS + " characters");
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
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "a speaker can submit at most " + TalkLimit.PER_SPEAKER + " talks");
        }
        notifications.talkSubmitted(speakerDao.findById(form.speakerId).orElseThrow().email(), form.title);
        return getTalk(id);
    }

    /** The constant with exactly this name, in capitals as declared; none for null or any other name. */
    private static <E extends Enum<E>> Optional<E> constantNamed(Class<E> type, String name) {
        return Arrays.stream(type.getEnumConstants()).filter(constant -> constant.name().equals(name)).findFirst();
    }

    private Long checkSubmissionLimit(TalkForm form, int duration) {
        return talkDao.insertIfBelow(TalkLimit.PER_SPEAKER, form.title, form.format, duration, form.level, form.speakerId,
                timestamps.now().toEpochMilli());
    }

    @Transactional(readOnly = true)
    public TalkResponse getTalk(long id) {
        return talkDao.findById(id).map(talk -> withRatings(List.of(talk)).get(0))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "talk not found"));
    }

    /** One query for the ratings of all the talks, not one per talk. */
    private List<TalkResponse> withRatings(List<Talk> talks) {
        Map<Long, RatingSummary> summaries = ratings.summariesFor(talks.stream().map(Talk::id).toList());
        return talks.stream()
                .map(talk -> TalkResponse.of(talk, summaries.getOrDefault(talk.id(), RatingSummary.NONE)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TalkResponse> getTalksForSpeaker(long speakerId) {
        if (speakerDao.findById(speakerId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "speaker not found");
        }
        return withRatings(talkDao.findBySpeaker(speakerId));
    }

    /** The talk list without the withdrawn talks when {@code includeWithdrawn} is false (Acceptance R9). */
    @Transactional(readOnly = true)
    public List<TalkResponse> listTalks(String sort, Long speakerId, boolean includeWithdrawn) {
        return listTalks(sort, speakerId).stream()
                .filter(talk -> includeWithdrawn || !isWithdrawn(talk))
                .collect(Collectors.toList());
    }

    /** The talks in the order of Ratings R5 or R6, with the withdrawn talks after the others (Acceptance R9). */
    @Transactional(readOnly = true)
    public List<TalkResponse> listTalks(String sort, Long speakerId) {
        Map<Boolean, List<TalkResponse>> byWithdrawn = orderedTalks(sort, speakerId).stream()
                .collect(Collectors.partitioningBy(TalkService::isWithdrawn));
        List<TalkResponse> talks = new ArrayList<>(byWithdrawn.get(false));
        talks.addAll(byWithdrawn.get(true));
        return talks;
    }

    private static boolean isWithdrawn(TalkResponse talk) {
        return talk.status() == TalkStatus.WITHDRAWN;
    }

    private List<TalkResponse> orderedTalks(String sort, Long speakerId) {
        if (sort != null && !sort.equals("rating")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sort must be 'rating'");
        }
        List<TalkResponse> talks = withRatings(speakerId == null ? talkDao.findAll() : talkDao.findBySpeaker(speakerId));
        if (!"rating".equals(sort)) {
            return talks;
        }
        Map<Boolean, List<TalkResponse>> byRated = talks.stream()
                .collect(Collectors.partitioningBy(talk -> talk.ratingCount() > 0));
        List<TalkResponse> ranked = new ArrayList<>(byRated.get(true).stream().sorted(RANKING).toList());
        ranked.addAll(byRated.get(false));
        return ranked;
    }
}
