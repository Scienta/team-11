package no.scienta.cfp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Statistics for the programme committee.
 */
@Service
public class ConferenceStatistics {

    private final SpeakerDao speakerDao;
    private final TalkService talkService;
    private final Timestamps timestamps;

    public ConferenceStatistics(SpeakerDao speakerDao, TalkService talkService, Timestamps timestamps) {
        this.speakerDao = speakerDao;
        this.talkService = talkService;
        this.timestamps = timestamps;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> statistics() {
        List<TalkResponse> talks = talkService.listTalks(TalkListQuery.EVERY_TALK);
        Map<String, Integer> byFormat = new LinkedHashMap<>();
        Map<String, Integer> byLevel = new LinkedHashMap<>();
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        Set<Long> registered = speakerDao.findAll().stream().map(Speaker::id).collect(Collectors.toSet());
        List<Long> speakersWithTalks = new ArrayList<>();
        int minutes = 0;
        double ratingSum = 0;
        int rated = 0;
        for (TalkResponse t : talks) {
            byFormat.put(t.format(), byFormat.getOrDefault(t.format(), 0) + 1);
            byLevel.put(t.level(), byLevel.getOrDefault(t.level(), 0) + 1);
            byStatus.put(t.status().name(), byStatus.getOrDefault(t.status().name(), 0) + 1);
            if (!speakersWithTalks.contains(t.speakerId()) && registered.contains(t.speakerId())) {
                speakersWithTalks.add(t.speakerId());
            }
            minutes += t.durationMinutes();
            if (t.averageRating() != null) {
                ratingSum += t.averageRating();
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
        stats.put("totalHours", Math.round(minutes / 60.0));
        stats.put("averageRating", rated == 0 ? null : RatingAverage.roundToOneDecimal(ratingSum / rated));
        stats.put("generated", Timestamps.databaseTime(timestamps.now()));
        return stats;
    }
}
