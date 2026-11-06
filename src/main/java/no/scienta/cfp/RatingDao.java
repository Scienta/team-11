package no.scienta.cfp;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RatingDao {

    private final JdbcTemplate jdbc;

    public RatingDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Insert or replace the reviewer's rating. One statement, so two clicks at once can't make two rows.
     */
    public void save(long talkId, long reviewerId, int score, String ratedAt) {
        jdbc.update("INSERT INTO rating (talk_id, reviewer_id, score, rated_at) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (talk_id, reviewer_id) DO UPDATE SET score = excluded.score, rated_at = excluded.rated_at",
                talkId, reviewerId, score, ratedAt);
    }

    /**
     * The rating totals of each rated talk among the given talks, read in one query. A talk with no rating is not in
     * the map.
     */
    public Map<Long, RatingTotals> totalsFor(List<Long> talkIds) {
        if (talkIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.query("SELECT talk_id, COUNT(*) AS cnt, SUM(score) AS total FROM rating "
                                + "WHERE talk_id = ANY(?) GROUP BY talk_id",
                        (rs, row) -> Map.entry(rs.getLong("talk_id"), new RatingTotals(rs.getInt("cnt"), rs.getLong("total"))),
                        (Object) talkIds.toArray(Long[]::new))
                .stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // public double average(long talkId) {
    //     return jdbc.queryForObject("SELECT AVG(score) FROM rating WHERE talk_id = ?", Double.class, talkId);
    // }
}
