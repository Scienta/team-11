package no.scienta.cfp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the programme: which talk is in which room and slot (scheduling). Reads the talks with
 * {@link TalkDao} and never changes them.
 */
@Repository
public class ProgrammeDao {

    private static final RowMapper<ScheduledTalk> SCHEDULED_TALK = (rs, row) -> {
        Set<Long> speakers = new HashSet<>(Arrays.asList((Long[]) rs.getArray("co_speaker_ids").getArray()));
        Long mainSpeaker = rs.getObject("speaker_id", Long.class);
        if (mainSpeaker != null) {
            speakers.add(mainSpeaker);
        }
        return new ScheduledTalk(Room.valueOf(rs.getString("room")), rs.getLong("talk_id"), speakers);
    };

    private static final RowMapper<Place> PLACE =
            (rs, row) -> new Place(Room.valueOf(rs.getString("room")), Slot.valueOf(rs.getString("slot")));

    private final JdbcTemplate jdbc;
    private final TalkDao talkDao;

    public ProgrammeDao(JdbcTemplate jdbc, TalkDao talkDao) {
        this.jdbc = jdbc;
        this.talkDao = talkDao;
    }

    /**
     * Locks the slot in all three rooms until the transaction ends, so that changes to one slot take turns and each
     * sees the talks that the one before put there (R8). Must run inside a transaction.
     */
    public void lockSlot(Slot slot) {
        jdbc.queryForList("SELECT slot FROM programme_slot WHERE slot = ? FOR UPDATE", String.class, slot.name());
    }

    /**
     * The talk, with its row locked against changes until the transaction ends, so that its status, format and
     * speakers stay as the rules saw them until the entry is saved (R8). Must run inside a transaction.
     */
    public Optional<ProgrammeTalk> lockTalk(long talkId) {
        return talkDao.lock(talkId, TalkDao.Lock.SHARE)
                ? talkDao.findById(talkId).map(ProgrammeTalk::of)
                : Optional.empty();
    }

    /** The talks in the slot, in all rooms, with their main speakers and co-speakers as they are now (R5, R6). */
    public SlotProgramme talksIn(Slot slot) {
        return new SlotProgramme(jdbc.query("SELECT e.room, e.talk_id, t.speaker_id::bigint AS speaker_id, "
                + "ARRAY(SELECT c.speaker_id::bigint FROM talk_co_speaker c WHERE c.talk_id = e.talk_id) "
                + "AS co_speaker_ids "
                + "FROM programme_entry e LEFT JOIN talk t ON t.id = e.talk_id WHERE e.slot = ?",
                SCHEDULED_TALK, slot.name()));
    }

    /** Where the talk is on the programme, if it is (R5). */
    public Optional<Place> placeOf(long talkId) {
        return jdbc.query("SELECT room, slot FROM programme_entry WHERE talk_id = ?", PLACE, talkId)
                .stream().findFirst();
    }

    /** The talk in the place, if there is one (R2). */
    public Optional<ProgrammeEntry> entryAt(Place place) {
        List<Long> talkIds = jdbc.queryForList("SELECT talk_id FROM programme_entry WHERE room = ? AND slot = ?",
                Long.class, place.room().name(), place.slot().name());
        return talkIds.stream().findFirst().map(talkId -> ProgrammeEntry.at(place, talkId));
    }

    /**
     * Puts the talk in the place, or saves nothing and returns false when the place is taken or the talk is on the
     * programme already. The unique indexes decide, so this holds also for two requests in different slots at the
     * same time (R5, R8). {@code ON CONFLICT} leaves the transaction usable, where a duplicate-key error would abort it.
     */
    public boolean add(Place place, long talkId) {
        return jdbc.update("INSERT INTO programme_entry (room, slot, talk_id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                place.room().name(), place.slot().name(), talkId) == 1;
    }

    /** Takes the talk out of the place. Returns whether a talk was there (R2). */
    public boolean remove(Place place) {
        return jdbc.update("DELETE FROM programme_entry WHERE room = ? AND slot = ?",
                place.room().name(), place.slot().name()) == 1;
    }
}
