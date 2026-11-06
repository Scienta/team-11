package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * A rating and a committee decision on the same talk at the same time (Acceptance R8): the rating waits for the
 * decision and is then refused, so no rating lands on a decided talk. The test holds the decision's transaction open
 * after its UPDATE, so the order of the two is fixed and does not depend on timing luck.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RatingDecisionRaceTest {

    private static final long WAIT_MS = 500;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.fresh(registry);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    TalkStatusDao talkStatusDao;

    @Autowired
    RatingService ratings;

    @DisplayName("R8: Refuses a rating that arrives while a decision closes the talk")
    @Test
    void refusesARatingThatArrivesWhileADecisionClosesTheTalk() throws Exception {
        long talk = submittedTalkRatedByTwoReviewers();
        CountDownLatch decided = new CountDownLatch(1);
        CountDownLatch commitDecision = new CountDownLatch(1);

        CompletableFuture<Void> decision = CompletableFuture.runAsync(() ->
                new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    assertTrue(talkStatusDao.moveTo(talk, TalkStatus.ACCEPTED));
                    decided.countDown();
                    awaitQuietly(commitDecision);
                }));
        assertTrue(decided.await(10, TimeUnit.SECONDS), "the decision did not run");

        CompletableFuture<Integer> rating = CompletableFuture.supplyAsync(() -> rateAsReviewer3(talk));
        Thread.sleep(WAIT_MS);
        assertFalse(rating.isDone(), "the rating did not wait for the open decision");

        commitDecision.countDown();
        decision.get(10, TimeUnit.SECONDS);
        assertEquals(409, rating.get(10, TimeUnit.SECONDS));
        assertEquals(2, ratingsOf(talk));
        assertEquals("ACCEPTED", jdbc.queryForObject("SELECT status FROM talk WHERE id = ?", String.class, talk));
    }

    private long submittedTalkRatedByTwoReviewers() {
        long speaker = jdbc.queryForObject("INSERT INTO speaker (name, email) VALUES ('Barbara Liskov', ?) RETURNING id",
                Long.class, "barbara-" + System.nanoTime() + "@example.com");
        long talk = jdbc.queryForObject("INSERT INTO talk (title, format, duration, level, status, speaker_id) "
                + "VALUES ('Data abstraction', 'PRESENTATION', 45, 'ADVANCED', 'SUBMITTED', ?) RETURNING id",
                Long.class, speaker);
        jdbc.update("INSERT INTO rating (talk_id, reviewer_id, score) VALUES (?, 1, 4), (?, 2, 5)", talk, talk);
        return talk;
    }

    /** The HTTP status that the rating would get: 200 when saved, else the refusal's status. */
    private int rateAsReviewer3(long talk) {
        try {
            ratings.rate(talk, 3, Map.of("score", 4));
            return 200;
        } catch (ResponseStatusException refused) {
            return refused.getStatusCode().value();
        }
    }

    private int ratingsOf(long talk) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM rating WHERE talk_id = ?", Integer.class, talk);
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
