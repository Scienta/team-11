package no.scienta.cfp;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Reads the talk that a registration is for, and reads and writes the workshop's registrations
 * (workshop-registration). Reads the talk table with its own SQL and never changes it.
 */
@Repository
public class WorkshopRegistrationDao {

    private static final RowMapper<WorkshopTalk> TALK =
            (rs, row) -> new WorkshopTalk(rs.getString("format"), rs.getString("status"));
    private static final RowMapper<Registration> REGISTRATION = (rs, row) -> new Registration(
            rs.getLong("id"), rs.getLong("talk_id"), rs.getString("name"), rs.getString("email"));

    private final JdbcTemplate jdbc;

    public WorkshopRegistrationDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * The talk, with its row locked until the transaction ends, so that registrations for one workshop take turns and
     * none can see a seat that another has just taken (R5, R6). Must run inside a transaction.
     */
    public Optional<WorkshopTalk> lockTalk(long talkId) {
        return jdbc.query("SELECT format, status FROM talk WHERE id = ? FOR UPDATE", TALK, talkId).stream().findFirst();
    }

    public Optional<WorkshopTalk> findTalk(long talkId) {
        return jdbc.query("SELECT format, status FROM talk WHERE id = ?", TALK, talkId).stream().findFirst();
    }

    public int seatsTaken(long talkId) {
        Integer taken = jdbc.queryForObject("SELECT COUNT(*) FROM workshop_registration WHERE talk_id = ?",
                Integer.class, talkId);
        return taken == null ? 0 : taken;
    }

    public boolean emailRegistered(long talkId, String email) {
        Integer found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM workshop_registration WHERE talk_id = ? AND lower(email) = lower(?)",
                Integer.class, talkId, email);
        return found != null && found > 0;
    }

    public Registration insert(long talkId, RegistrationRequest request) {
        long id = jdbc.queryForObject(
                "INSERT INTO workshop_registration (talk_id, name, email) VALUES (?, ?, ?) RETURNING id",
                Long.class, talkId, request.name(), request.email());
        return new Registration(id, talkId, request.name(), request.email());
    }

    /** The workshop's registrations in the order they were saved (R8). */
    public List<Registration> registrations(long talkId) {
        return List.copyOf(jdbc.query(
                "SELECT id, talk_id, name, email FROM workshop_registration WHERE talk_id = ? ORDER BY id",
                REGISTRATION, talkId));
    }
}
