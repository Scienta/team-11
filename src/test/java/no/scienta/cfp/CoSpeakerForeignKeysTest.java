package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The foreign keys of talk_co_speaker (Co-speakers R11) on a database from before them, as production has it: the
 * table exists without keys and has links to a talk and a speaker that are gone. The app starts on that database, so
 * schema.sql runs as it does on a deploy.
 */
@DisplayName("Co-speakers: foreign keys on an existing database")
@SpringBootTest
class CoSpeakerForeignKeysTest {

    private static final ClassPathResource SCHEMA = new ClassPathResource("schema.sql");
    private static final String DROP_FOREIGN_KEYS = "ALTER TABLE talk_co_speaker"
            + " DROP CONSTRAINT IF EXISTS talk_co_speaker_talk_id_fkey,"
            + " DROP CONSTRAINT IF EXISTS talk_co_speaker_speaker_id_fkey";
    private static final String FOREIGN_KEYS = "SELECT conname || ' ' || convalidated || ' ' || confdeltype::text"
            + " FROM pg_constraint WHERE conrelid = 'talk_co_speaker'::regclass AND contype = 'f' ORDER BY conname";
    private static final String LINKS = "SELECT talk_id || ' ' || speaker_id FROM talk_co_speaker";
    private static final List<String> VALID_CASCADING_KEYS =
            List.of("talk_co_speaker_speaker_id_fkey true c", "talk_co_speaker_talk_id_fkey true c");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestDatabase.freshWith(registry, CoSpeakerForeignKeysTest::tableFromBeforeTheForeignKeys);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DataSource dataSource;

    /** Talk 1 by speaker 1 with speaker 2 as co-speaker, and links to talk 99 and speaker 99, which don't exist. */
    private static void tableFromBeforeTheForeignKeys(Connection c) throws SQLException {
        ScriptUtils.executeSqlScript(c, SCHEMA);
        try (Statement s = c.createStatement()) {
            s.execute(DROP_FOREIGN_KEYS);
            s.execute("INSERT INTO speaker (id, name, email) VALUES (1, 'Main', 'main@example.com'),"
                    + " (2, 'Ada', 'ada@example.com')");
            s.execute("INSERT INTO talk (id, title, format, speaker_id) VALUES (1, 'Kept', 'PRESENTATION', 1)");
            s.execute("INSERT INTO talk_co_speaker (talk_id, speaker_id) VALUES (1, 2), (1, 99), (99, 2)");
        }
    }

    @Test
    @DisplayName("R11: Start-up deletes the links to a talk or speaker that is gone, and keeps the others")
    void startUpDeletesTheOrphanLinks() {
        assertEquals(List.of("1 2"), jdbc.queryForList(LINKS, String.class));
    }

    @Test
    @DisplayName("R11: Start-up adds both foreign keys, validated and cascading")
    void startUpAddsTheForeignKeys() {
        assertEquals(VALID_CASCADING_KEYS, jdbc.queryForList(FOREIGN_KEYS, String.class));
    }

    @Test
    @DisplayName("R11: Running the schema again changes nothing")
    void runningTheSchemaAgainChangesNothing() {
        new ResourceDatabasePopulator(SCHEMA).execute(dataSource);
        new ResourceDatabasePopulator(SCHEMA).execute(dataSource);
        assertEquals(VALID_CASCADING_KEYS, jdbc.queryForList(FOREIGN_KEYS, String.class));
        assertEquals(List.of("1 2"), jdbc.queryForList(LINKS, String.class));
    }

    @Test
    @DisplayName("R11: When a foreign key can't be added, the schema still runs to the end, without the keys")
    void aForeignKeyThatCantBeAddedDoesNotStopTheSchema() {
        TestDatabase.inNewDatabase(c -> {
            ScriptUtils.executeSqlScript(c, SCHEMA);
            try (Statement s = c.createStatement()) {
                s.execute(DROP_FOREIGN_KEYS);
                s.execute("ALTER TABLE talk DROP CONSTRAINT talk_pkey");
            }
            ScriptUtils.executeSqlScript(c, SCHEMA);
            assertEquals(List.of(), foreignKeys(c));
        });
    }

    private static List<String> foreignKeys(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(FOREIGN_KEYS)) {
            List<String> keys = new ArrayList<>();
            while (rs.next()) {
                keys.add(rs.getString(1));
            }
            return keys;
        }
    }
}
