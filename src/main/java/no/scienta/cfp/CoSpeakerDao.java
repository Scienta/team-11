package no.scienta.cfp;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CoSpeakerDao {

    private final JdbcTemplate jdbc;

    public CoSpeakerDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Adds the co-speaker last. Does nothing when they already are one, so a double-click can't make two rows. */
    public void add(long talkId, long speakerId) {
        jdbc.update("INSERT INTO talk_co_speaker (talk_id, speaker_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                talkId, speakerId);
    }

    public void remove(long talkId, long speakerId) {
        jdbc.update("DELETE FROM talk_co_speaker WHERE talk_id = ? AND speaker_id = ?", talkId, speakerId);
    }
}
