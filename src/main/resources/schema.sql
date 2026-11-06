-- CFP schema. Runs on every start-up (spring.sql.init.mode=always), so everything must be IF NOT EXISTS.
-- Don't edit old statements, add new ones at the bottom.

-- 2019-03 initial version
CREATE TABLE IF NOT EXISTS speaker (
    id         SERIAL PRIMARY KEY,
    name       TEXT NOT NULL,
    email      TEXT NOT NULL,
    bio        TEXT,
    created    TEXT          -- dd.MM.yyyy, the old admin UI wrote it like that
);

-- 2019-04 talks
CREATE TABLE IF NOT EXISTS talk (
    id           SERIAL PRIMARY KEY,
    title        TEXT NOT NULL,
    abstract     TEXT,                              -- never exposed, the web form was dropped
    format       TEXT NOT NULL,
    duration     INTEGER,                           -- derived from format, the reporting tool reads it
    level        TEXT,
    status       TEXT NOT NULL DEFAULT 'SUBMITTED',
    speaker_id   INTEGER NOT NULL,
    submitted_at BIGINT                             -- epoch millis (UTC)
);

-- 2019-10 voting, replaced by rating in 2020. Kept for the 2019 statistics.
CREATE TABLE IF NOT EXISTS vote (
    talk_id INTEGER,
    voter   TEXT,
    stars   INTEGER
);

-- 2020-02 ratings
CREATE TABLE IF NOT EXISTS rating (
    talk_id     INTEGER NOT NULL,
    reviewer_id INTEGER NOT NULL,
    score       INTEGER NOT NULL,
    rated_at    TEXT,                               -- yyyy-MM-dd HH:mm:ss (UTC)
    PRIMARY KEY (talk_id, reviewer_id)
);

-- 2020-06 programme planning (never finished)
-- ALTER TABLE talk ADD COLUMN room TEXT;
-- ALTER TABLE talk ADD COLUMN slot TEXT;

-- 2021-09 after the CSV import created duplicate speakers
CREATE UNIQUE INDEX IF NOT EXISTS ux_speaker_email ON speaker (lower(email));

-- 2022-01 GET /speakers/{id}/talks was slow
CREATE INDEX IF NOT EXISTS ix_talk_speaker ON talk (speaker_id);

-- 2026-11 a reviewer is any positive whole number (Ratings R1), not only up to 2147483647.
-- Only the first run rewrites the table; when the column is already BIGINT, Postgres does nothing.
ALTER TABLE rating ALTER COLUMN reviewer_id TYPE BIGINT;

-- 2026-11 co-speakers: other registered speakers on a talk, in the order they were added. No foreign keys:
-- orphan rows from before would make start-up fail.
CREATE TABLE IF NOT EXISTS talk_co_speaker (
    talk_id     INTEGER NOT NULL,
    speaker_id  INTEGER NOT NULL,
    added_order BIGSERIAL,
    PRIMARY KEY (talk_id, speaker_id)
);

-- 2026-11 workshop registration (workshop-registration R1-R8). No foreign key, like the other tables.
-- The unique index backs "one registration per email per workshop, whatever the letter case" (R6).
CREATE TABLE IF NOT EXISTS workshop_registration (
    id      BIGSERIAL PRIMARY KEY,
    talk_id BIGINT NOT NULL,
    name    TEXT NOT NULL,
    email   TEXT NOT NULL                          -- as the attendee typed it
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_workshop_registration_email ON workshop_registration (talk_id, lower(email));

-- 2026-11 programme scheduling (scheduling R1-R8). No foreign key, like the other tables.
-- One fixed row per slot: a PUT or DELETE locks it with FOR UPDATE, so the changes to one slot in all three rooms
-- take turns (R6, R8). Advisory locks are not available on the platform.
CREATE TABLE IF NOT EXISTS programme_slot (
    slot TEXT PRIMARY KEY
);
INSERT INTO programme_slot (slot) VALUES ('S1'), ('S2'), ('S3'), ('S4'), ('S5') ON CONFLICT DO NOTHING;
-- The primary key backs "one talk per room and slot", the unique index "a talk is on the programme only once" (R5).
CREATE TABLE IF NOT EXISTS programme_entry (
    room    TEXT NOT NULL,
    slot    TEXT NOT NULL,
    talk_id BIGINT NOT NULL,
    PRIMARY KEY (room, slot)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_programme_entry_talk ON programme_entry (talk_id);

-- 2026-11 cancellation token (workshop-registration R10): only its SHA-256 hash. Older rows keep NULL and cannot be
-- unregistered with a token.
ALTER TABLE workshop_registration ADD COLUMN IF NOT EXISTS cancellation_token_hash TEXT;
