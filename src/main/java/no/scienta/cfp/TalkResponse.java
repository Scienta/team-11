package no.scienta.cfp;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A talk as the API returns it: the stored talk with its ratings.
 */
@JsonPropertyOrder({"id", "title", "format", "level", "speakerId", "durationMinutes", "status", "submittedAt",
        "submittedOn", "averageRating", "ratingCount", "coSpeakerIds"})
public record TalkResponse(long id, String title, String format, String level, long speakerId, int durationMinutes,
                           TalkStatus status, Date submittedAt, java.sql.Date submittedOn, Double averageRating,
                           int ratingCount, List<Long> coSpeakerIds) {

    static TalkResponse of(Talk talk, RatingSummary ratings) {
        return new TalkResponse(talk.id(), talk.title(), talk.format(), talk.level(), talk.speakerId(),
                talk.durationMinutes(), talk.status(), talk.submittedAt(), talk.submittedOn(), ratings.average(),
                ratings.count(), talk.coSpeakerIds());
    }
}
