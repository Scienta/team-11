package no.scienta.cfp;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

    private final RowMapper<TalkDto> mapper = (rs, rowNum) -> {
        TalkDto t = new TalkDto();
        t.id = rs.getLong("id");
        t.title = rs.getString("title");
        t.format = rs.getString("format");
        t.durationMinutes = rs.getInt("duration");
        t.level = rs.getString("level");
        t.status = TalkStatus.valueOf(rs.getString("status"));
        t.speakerId = rs.getLong("speaker_id");
        long millis = rs.getLong("submitted_at");
        t.submittedAt = new Date(millis);
        t.submittedOn = Utils.toSqlDate(millis);
        t.coSpeakerIds = List.of((Long[]) rs.getArray("co_speaker_ids").getArray());
        return t;
    };

    public TalkDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public TalkDto findById(long id) {
        List<TalkDto> list = jdbc.query("SELECT " + COLUMNS + " FROM talk WHERE id = ?", mapper, id);
        return list.isEmpty() ? null : list.get(0);
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

    /** The talk, read after {@link #lock} with {@link Lock#UPDATE}, or null when there is no such talk. */
    public TalkDto findByIdForUpdate(long id) {
        lock(id, Lock.UPDATE);
        return findById(id);
    }

    public List<TalkDto> findBySpeaker(long speakerId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM talk WHERE speaker_id = ? ORDER BY id", mapper, speakerId);
    }

    public List<TalkDto> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM talk ORDER BY id", mapper);
    }

    public boolean exists(long id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM talk WHERE id = ?", Integer.class, id);
        return n != null && n > 0;
    }

    /**
     * Inserts the talk only if the speaker has fewer than {@code max} talks that count towards the limit
     * ({@link TalkStatus#countsTowardLimit()}). Returns the new id, or null.
     * The speaker row is locked first so two submissions for the same speaker can't both see room under the limit.
     */
    @Transactional
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

    // old version, raced when the mobile app double-posted
    // public long insert(String title, String format, int duration, String level, long speakerId) {
    //     jdbc.update("INSERT INTO talk (title, format, duration, level, status, speaker_id) VALUES (?, ?, ?, ?, 'SUBMITTED', ?)",
    //             title, format, duration, level, speakerId);
    //     return jdbc.queryForObject("SELECT currval('talk_id_seq')", Long.class);
    // }
}
