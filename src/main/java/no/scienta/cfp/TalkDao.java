package no.scienta.cfp;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class TalkDao {

    private static final String COLUMNS = "id, title, format, duration, level, status, speaker_id, submitted_at, "
            + "ARRAY(SELECT c.speaker_id::bigint FROM talk_co_speaker c WHERE c.talk_id = talk.id ORDER BY c.added_order)"
            + " AS co_speaker_ids";

    private final JdbcTemplate jdbc;

    private final RowMapper<Talk> mapper = (rs, rowNum) -> {
        long millis = rs.getLong("submitted_at");
        return new Talk(rs.getLong("id"), rs.getString("title"), rs.getString("format"), rs.getString("level"),
                rs.getLong("speaker_id"), rs.getInt("duration"), rs.getString("status"), new Date(millis),
                Utils.toSqlDate(millis), List.of((Long[]) rs.getArray("co_speaker_ids").getArray()));
    };

    public TalkDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Talk> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM talk WHERE id = ?", mapper, id).stream().findFirst();
    }

    /**
     * Locks the talk row until the transaction ends, so changes to one talk happen one at a time. The talk is read in a
     * second statement: one that waited for the lock still sees the co-speakers from before the wait.
     */
    public Optional<Talk> findByIdForUpdate(long id) {
        jdbc.queryForList("SELECT id FROM talk WHERE id = ? FOR UPDATE", id);
        return findById(id);
    }

    public List<Talk> findBySpeaker(long speakerId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM talk WHERE speaker_id = ? ORDER BY id", mapper, speakerId);
    }

    public List<Talk> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM talk ORDER BY id", mapper);
    }

    public boolean exists(long id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM talk WHERE id = ?", Integer.class, id);
        return n != null && n > 0;
    }

    /**
     * Inserts the talk only if the speaker has fewer than {@code max} talks that count. A withdrawn talk does not
     * count (Acceptance R7). Returns the new id, or null.
     * The speaker row is locked first so two submissions for the same speaker can't both see room under the limit.
     */
    @Transactional
    public Long insertIfBelow(int max, String title, String format, int duration, String level, long speakerId, long submittedAt) {
        jdbc.queryForList("SELECT id FROM speaker WHERE id = ? FOR UPDATE", speakerId);
        List<Long> ids = jdbc.queryForList(
                "INSERT INTO talk (title, format, duration, level, status, speaker_id, submitted_at) "
                        + "SELECT ?, ?, ?, ?, 'SUBMITTED', ?, ? "
                        + "WHERE (SELECT COUNT(*) FROM talk WHERE speaker_id = ? AND status <> 'WITHDRAWN') < ? RETURNING id",
                Long.class, title, format, duration, level, speakerId, submittedAt, speakerId, max);
        return ids.isEmpty() ? null : ids.get(0);
    }
}
