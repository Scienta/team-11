package no.scienta.cfp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Registering, looking up, updating and searching speakers.
 */
@Service
public class SpeakerService {

    private static final Logger log = LoggerFactory.getLogger(SpeakerService.class);

    private static final int MAX_BIO_CHARACTERS = 500;

    private final SpeakerDao speakerDao;
    private final NotificationService notifications;

    public SpeakerService(SpeakerDao speakerDao, NotificationService notifications) {
        this.speakerDao = speakerDao;
        this.notifications = notifications;
    }

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
}
