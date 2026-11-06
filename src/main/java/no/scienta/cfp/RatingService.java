package no.scienta.cfp;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reviewers rating talks, and the rating count and average each talk shows.
 */
@Service
public class RatingService {

    private final TalkStatusDao talkStatusDao;
    private final RatingDao ratingDao;

    public RatingService(TalkStatusDao talkStatusDao, RatingDao ratingDao) {
        this.talkStatusDao = talkStatusDao;
        this.ratingDao = ratingDao;
    }

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

    public RatingSummary summaryOf(long talkId) {
        Map<String, Object> stats = ratingDao.stats(talkId);
        return RatingSummary.of(((Number) stats.get("cnt")).intValue(), ((Number) stats.get("total")).longValue());
    }
}
