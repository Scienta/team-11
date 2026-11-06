package no.scienta.cfp;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SpeakerDao {

    private static final RowMapper<Speaker> SPEAKER = (rs, rowNum) ->
            new Speaker(rs.getLong("id"), rs.getString("name"), rs.getString("email"), rs.getString("bio"));

    private final JdbcTemplate jdbc;
    private final Timestamps timestamps;

    public SpeakerDao(JdbcTemplate jdbc, Timestamps timestamps) {
        this.jdbc = jdbc;
        this.timestamps = timestamps;
    }

    public Optional<Speaker> findById(long id) {
        return jdbc.query("SELECT id, name, email, bio FROM speaker WHERE id = ?", SPEAKER, id).stream().findFirst();
    }

    public boolean exists(long id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM speaker WHERE id = ?", Integer.class, id);
        return n != null && n > 0;
    }

    public boolean emailTaken(String email) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM speaker WHERE lower(email) = lower(?)", Integer.class, email);
        return n != null && n > 0;
    }

    public long insert(String name, String email, String bio) {
        return jdbc.queryForObject(
                "INSERT INTO speaker (name, email, bio, created) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, name, email, bio, timestamps.today());
    }

    public int update(long id, String name, String email, String bio) {
        return jdbc.update("UPDATE speaker SET name = ?, email = ?, bio = ? WHERE id = ?", name, email, bio, id);
    }

    public List<Speaker> findAll() {
        return jdbc.query("SELECT id, name, email, bio FROM speaker", SPEAKER);
    }

    public int count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM speaker", Integer.class);
    }

    // Used by the old admin page to look people up.
    public List<Speaker> search(String name) {
        return jdbc.query(
                "SELECT id, name, email, bio FROM speaker WHERE name ILIKE '%' || ? || '%' ORDER BY name", SPEAKER, name);
    }
}
