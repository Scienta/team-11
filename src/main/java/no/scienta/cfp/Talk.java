package no.scienta.cfp;

import java.util.Date;
import java.util.List;

/**
 * A talk as stored. {@code submittedOn} is the UTC day of {@code submittedAt}; the reporting tool wants a plain date.
 */
public record Talk(long id, String title, String format, String level, long speakerId, int durationMinutes,
                   TalkStatus status, Date submittedAt, java.sql.Date submittedOn, List<Long> coSpeakerIds) {
}
