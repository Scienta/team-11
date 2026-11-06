package no.scienta.cfp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Reads everything on the programme with the talks and the speakers' names as they are now (programme-export R1, R3,
 * R4). Never changes anything. An entry without a talk row, or a speaker without a speaker row, is left out.
 */
@Repository
public class ProgrammeExportDao {

    private static final RowMapper<ProgrammeRow> ROW = (rs, row) -> {
        List<String> speakers = new ArrayList<>();
        String mainSpeaker = rs.getString("main_speaker");
        if (mainSpeaker != null) {
            speakers.add(mainSpeaker);
        }
        speakers.addAll(Arrays.asList((String[]) rs.getArray("co_speakers").getArray()));
        return new ProgrammeRow(Slot.valueOf(rs.getString("slot")), Room.valueOf(rs.getString("room")),
                rs.getLong("talk_id"), rs.getString("title"), rs.getString("format"), speakers);
    };

    private final JdbcTemplate jdbc;

    public ProgrammeExportDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every talk on the programme, in no particular order; {@link Programme} puts them in the order of the day. */
    public List<ProgrammeRow> rows() {
        return List.copyOf(jdbc.query("SELECT e.slot, e.room, t.id AS talk_id, t.title, t.format, "
                + "m.name AS main_speaker, "
                + "ARRAY(SELECT s.name FROM talk_co_speaker c JOIN speaker s ON s.id = c.speaker_id "
                + "WHERE c.talk_id = t.id ORDER BY c.added_order) AS co_speakers "
                + "FROM programme_entry e JOIN talk t ON t.id = e.talk_id LEFT JOIN speaker m ON m.id = t.speaker_id",
                ROW));
    }
}
