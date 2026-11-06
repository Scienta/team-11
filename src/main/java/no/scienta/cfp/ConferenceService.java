package no.scienta.cfp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Speakers, talks and ratings.
 *
 * TODO split this up (CFP-140)
 */
@Service
public class ConferenceService {

    private static final Logger log = LoggerFactory.getLogger(ConferenceService.class);

    private static final int MAX_BIO_CHARACTERS = 500;

    private static final int MAX_TALKS_PER_SPEAKER = 3;

    /** The order the 400 message has always listed the formats in; clients may compare the message. */
    private static final List<TalkFormat> FORMATS_IN_ERROR_MESSAGE = List.of(
            TalkFormat.WORKSHOP, TalkFormat.PRESENTATION, TalkFormat.LIGHTNING_20, TalkFormat.LIGHTNING_10);

    private final SpeakerDao speakerDao;
    private final TalkDao talkDao;
    private final RatingDao ratingDao;
    private final TalkStatusDao talkStatusDao;
    private final NotificationService notifications;

    public ConferenceService(SpeakerDao speakerDao, TalkDao talkDao, RatingDao ratingDao,
                             TalkStatusDao talkStatusDao, NotificationService notifications) {
        this.speakerDao = speakerDao;
        this.talkDao = talkDao;
        this.ratingDao = ratingDao;
        this.talkStatusDao = talkStatusDao;
        this.notifications = notifications;
    }

    // ---------------------------------------------------------------- speakers

    @Transactional
    public Map<String, Object> registerSpeaker(SpeakerForm form) {
        if (!Utils.isValidEmail(form.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid email");
        }
        requireBioWithinLimit(form.getBio());
        if (speakerDao.emailTaken(form.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }
        long id;
        try {
            id = speakerDao.insert(form.getName(), form.getEmail(), form.getBio());
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }
        log.info("registered speaker " + id + " (" + form.getEmail() + ")");
        notifications.speakerRegistered(form.getEmail(), form.getName());
        Map<String, Object> speaker = new LinkedHashMap<>();
        speaker.put("id", id);
        speaker.put("name", form.getName());
        speaker.put("email", form.getEmail());
        speaker.put("bio", form.getBio());
        return speaker;
    }

    public Map<String, Object> getSpeaker(long id) {
        Map<String, Object> speaker = speakerDao.findById(id);
        if (speaker == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "speaker not found");
        }
        return speaker;
    }

    @Transactional
    public Map<String, Object> updateSpeaker(long id, String name, String email, String bio) {
        Map<String, Object> existing = getSpeaker(id);
        if (name == null || name.equals("")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (!Utils.isValidEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid email");
        }
        requireBioWithinLimit(bio);
        if (!email.equalsIgnoreCase((String) existing.get("email")) && speakerDao.emailTaken(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }
        speakerDao.update(id, name, email, bio);
        return getSpeaker(id);
    }

    /** A character is one Unicode code point, so an emoji counts once. */
    private static void requireBioWithinLimit(String bio) {
        if (bio != null && bio.codePointCount(0, bio.length()) > MAX_BIO_CHARACTERS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "bio is longer than " + MAX_BIO_CHARACTERS + " characters");
        }
    }

    public List<Map<String, Object>> searchSpeakers(String q) {
        return speakerDao.search(q);
    }

    // ---------------------------------------------------------------- talks

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
        Map<String, Object> speaker = speakerDao.findById(form.speakerId);
        notifications.talkSubmitted((String) speaker.get("email"), form.title);
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
        addRatings(talk);
        return talk;
    }

    public List<TalkDto> getTalksForSpeaker(long speakerId) {
        if (speakerDao.findById(speakerId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "speaker not found");
        }
        List<TalkDto> talks = talkDao.findBySpeaker(speakerId);
        for (TalkDto t : talks) {
            addRatings(t);
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
                .collect(Collectors.partitioningBy(ConferenceService::isWithdrawn));
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
            addRatings(t);
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

    private void addRatings(TalkDto talk) {
        Map<String, Object> stats = ratingDao.stats(talk.id);
        int count = ((Number) stats.get("cnt")).intValue();
        long total = ((Number) stats.get("total")).longValue();
        talk.ratingCount = count;
        talk.averageRating = count == 0 ? null : calculateAverage(total, count);
    }

    // 1 decimal, half up (4.25 -> 4.3)
    private Double calculateAverage(long total, int count) {
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP).doubleValue();
    }

    // ---------------------------------------------------------------- statistics

    public Map<String, Object> statistics() {
        List<TalkDto> talks = listTalks(null, null);
        Map<String, Integer> byFormat = new LinkedHashMap<>();
        Map<String, Integer> byLevel = new LinkedHashMap<>();
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        List<Long> speakersWithTalks = new ArrayList<>();
        int minutes = 0;
        double ratingSum = 0;
        int rated = 0;
        for (TalkDto t : talks) {
            byFormat.put(t.format, byFormat.getOrDefault(t.format, 0) + 1);
            byLevel.put(t.level, byLevel.getOrDefault(t.level, 0) + 1);
            byStatus.put(t.status, byStatus.getOrDefault(t.status, 0) + 1);
            if (!speakersWithTalks.contains(t.speakerId) && speakerDao.findById(t.speakerId) != null) {
                speakersWithTalks.add(t.speakerId);
            }
            minutes += t.durationMinutes;
            if (t.averageRating != null) {
                ratingSum += t.averageRating;
                rated++;
            }
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("speakers", speakerDao.count());
        stats.put("speakersWithTalks", speakersWithTalks.size());
        stats.put("talks", talks.size());
        stats.put("byFormat", byFormat);
        stats.put("byLevel", byLevel);
        stats.put("byStatus", byStatus);
        stats.put("totalHours", minutes / 60);
        stats.put("averageRating", rated == 0 ? null : Utils.roundRating(ratingSum / rated));
        stats.put("generated", Utils.now());
        return stats;
    }

    // ---------------------------------------------------------------- ratings

    @Transactional
    public Map<String, Object> rate(long talkId, long reviewerId, Map<String, Object> body) {
        if (reviewerId < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reviewerId must be positive");
        }
        Object score = body.get("score");
        if (!(score instanceof Integer)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "score must be a whole number");
        }
        int s = (Integer) score;
        if (s < 1 || s > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "score must be 1-5");
        }
        TalkStatus status = talkStatusDao.lockStatus(talkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "talk not found"));
        if (!status.isOpenForRating()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "a talk that is " + status + " takes no ratings");
        }
        String now = Utils.now();
        ratingDao.save(talkId, reviewerId, s, now);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("talkId", talkId);
        result.put("reviewerId", reviewerId);
        result.put("score", s);
        result.put("ratedAtDisplay", Utils.usShort(Utils.parse(now)));
        return result;
    }
}
