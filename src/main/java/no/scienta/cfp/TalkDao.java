package no.scienta.cfp;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TalkDao {

    private static final String COLUMNS = "id, title, format, duration, level, status, speaker_id, submitted_at, "
            + "ARRAY(SELECT c.speaker_id::bigint FROM talk_co_speaker c WHERE c.talk_id = talk.id ORDER BY c.added_order)"
            + " AS co_speaker_ids";

    private final JdbcTemplate jdbc;

    private final RowMapper<Talk> mapper = (rs, rowNum) -> {
        long millis = rs.getLong("submitted_at");
        return new Talk(rs.getLong("id"), rs.getString("title"), rs.getString("format"), rs.getString("level"),
                rs.getLong("speaker_id"), rs.getInt("duration"), TalkStatus.valueOf(rs.getString("status")), new Date(millis),
                utcDay(millis), List.of((Long[]) rs.getArray("co_speaker_ids").getArray()));
    };

    public TalkDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static java.sql.Date utcDay(long epochMillis) {
        return java.sql.Date.valueOf(Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate());
    }

    public Optional<Talk> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM talk WHERE id = ?", mapper, id).stream().findFirst();
    }

    /** How a transaction holds a talk row until it ends. */
    public enum Lock {
        /** Blocks changes to the talk, but lets other readers take the same lock. */
        SHARE("FOR SHARE"),
        /** Blocks changes and every other lock on the talk, so changes to one talk happen one at a time. */
        UPDATE("FOR UPDATE");

        private final String clause;

        Lock(String clause) {
            this.clause = clause;
        }
    }

    /**
     * Locks the talk row until the transaction ends, and returns whether the talk exists. Read the talk in a later
     * statement: a statement that waited for the lock still sees other tables, such as the co-speakers, as they were
     * before the wait. Must run inside a transaction.
     */
    public boolean lock(long id, Lock mode) {
        return !jdbc.queryForList("SELECT id FROM talk WHERE id = ? " + mode.clause, Long.class, id).isEmpty();
    }

    /** The talk, read after {@link #lock} with {@link Lock#UPDATE}, or empty when there is no such talk. */
    public Optional<Talk> findByIdForUpdate(long id) {
        lock(id, Lock.UPDATE);
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
     * Inserts the talk only if the speaker has fewer than {@code max} talks that count towards the limit
     * ({@link TalkStatus#countsTowardLimit()}). Returns the new id, or null.
     * The speaker row is locked first so two submissions for the same speaker can't both see room under the limit. The
     * caller's transaction holds the lock, so call this inside one.
     */
    public Long insertIfBelow(int max, String title, String format, int duration, String level, long speakerId, long submittedAt) {
        jdbc.queryForList("SELECT id FROM speaker WHERE id = ? FOR UPDATE", speakerId);
        String[] outsideTheLimit = Arrays.stream(TalkStatus.values())
                .filter(status -> !status.countsTowardLimit())
                .map(Enum::name)
                .toArray(String[]::new);
        List<Long> ids = jdbc.queryForList(
                "INSERT INTO talk (title, format, duration, level, status, speaker_id, submitted_at) "
                        + "SELECT ?, ?, ?, ?, 'SUBMITTED', ?, ? "
                        + "WHERE (SELECT COUNT(*) FROM talk WHERE speaker_id = ? AND status <> ALL(?)) < ? "
                        + "RETURNING id",
                Long.class, title, format, duration, level, speakerId, submittedAt, speakerId, outsideTheLimit, max);
        return ids.isEmpty() ? null : ids.get(0);
    }
}
