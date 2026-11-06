package no.scienta.cfp;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads and changes the status of a talk so that concurrent requests never break the acceptance rules (R6). Each
 * change is one conditional UPDATE, because the platform has no advisory locks.
 */
@Repository
public class TalkStatusDao {

    private final JdbcTemplate jdbc;

    public TalkStatusDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Moves the talk to the status only if its current status allows the move and enough different reviewers have
     * rated it. Returns whether the talk moved. A reviewer has one rating row per talk, so the rows count reviewers.
     */
    public boolean moveTo(long talkId, TalkStatus next) {
        List<String> from = next.reachableFrom().stream().map(Enum::name).collect(Collectors.toList());
        if (from.isEmpty()) {
            return false;
        }
        String placeholders = from.stream().map(status -> "?").collect(Collectors.joining(", "));
        Object[] args = Stream.of(Stream.of(next.name(), talkId), from.stream(), Stream.of(talkId, next.reviewersNeeded()))
                .flatMap(Function.identity())
                .toArray();
        int moved = jdbc.update("UPDATE talk SET status = ? WHERE id = ? AND status IN (" + placeholders + ") "
                + "AND (SELECT COUNT(*) FROM rating WHERE talk_id = ?) >= ?", args);
        return moved == 1;
    }

    /**
     * Reads the talk's status and keeps the row locked against a status change until the transaction ends, so that
     * a rating cannot land on a talk that a decision has just closed (R8). Must run inside a transaction.
     */
    public Optional<TalkStatus> lockStatus(long talkId) {
        List<String> status = jdbc.queryForList("SELECT status FROM talk WHERE id = ? FOR SHARE", String.class, talkId);
        return status.stream().findFirst().map(TalkStatus::valueOf);
    }
}
