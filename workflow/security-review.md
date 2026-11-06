# Security review

Ferdinand, the security guardian session on this laptop, keeps this file. The guardian does not write fixes, so
that the review stays independent. Other sessions write the fixes. The guardian reviews each batch of commits
before a push, and runs the exploit again against a local build of each fix.

## How to review a batch

1. Read `git diff <range> -- src/ pom.xml Dockerfile src/main/resources/` against the checklist below.
2. Build the last commit of the range from `git archive` in `$TMPDIR`, not in the shared `target/`.
3. Run it with `java -Xmx256m -jar target/cfp.jar` on local Postgres, and run the exploit script
   (`exploits.sh http://localhost:8080 --ref <sha> --log <app log> --big`).
4. Answer The Hand with "no blocker" or the list of blockers, at most 10 lines.

## Checklist

| ID | Check | A blocker when |
|----|-------|----------------|
| C1 | SQL | A value from the request goes into SQL text. A joined list of `?` from enum values is safe. |
| C2 | Authorisation | A new endpoint changes data and is in no spec. An endpoint in a spec without an auth model is a PO question, not a blocker. |
| C3 | Error answers | A 500 can come from bad input, or an answer contains SQL, `PSQLException` or a stack trace. |
| C4 | Input size | A request body, string or list has no upper limit before it reaches memory or the database. |
| C5 | Personal data | An email or name goes into a log line, or into an answer that the spec does not ask for. |
| C6 | Limits under concurrency | A limit (3 talks, 2 co-speakers, 20 seats) is a read followed by a write without a row lock or a conditional write. |
| C7 | Exports | A user value goes into the CSV without formula and line-break protection. |
| C8 | Dependencies and image | A new dependency, a version with a known CVE on a used feature, a secret in the repo, or a root user in the `Dockerfile`. |

## Findings

From the review of HEAD `d66b3df` on 2026-11-06. The "Where" column gives the lines at `d66b3df`. B1 (SQL injection in `/speakers/search`) is fixed by Carl
Kristian's team. Co-speakers is reviewed by Carl Kristian's team.

| ID | Severity | Finding | Where | Decision | Fix | Exploit re-run |
|----|----------|---------|-------|----------|-----|----------------|
| S1 | High | Anybody can change any speaker with `PUT /speakers/{id}`. It is in no spec. | `SpeakerController.java:41` | `c4c6205` | CLOSED on `b7f1b79` |
| S14 | Low | `/programme.csv` writes titles and speaker names without the formula guard, so a title such as `=HYPERLINK(…)` can run as a formula in a spreadsheet. | Programme export (spec `workflow/specs/programme-export/spec.md`) | Accepted (The Hand, 2026-11-06): the request says fields are "otherwise written as they are", the harness may check exact bytes, and the file goes to the print shop, not to a spreadsheet. `/admin/talks.csv` keeps its guard (S6). Still required (C7): RFC 4180 quoting for a comma, a quote, CR or LF in any field, so that no value starts a new row. | none | accepted; quoting is checked in the code review |
| S16 | Low | `/programme.csv` quotes a field only for a comma, a quote, CR or LF. A title or name with U+2028, U+2029, U+0085, VT or FF is not quoted, so a tool that splits lines on them sees broken rows. RFC 4180 parsers read it correctly. | `ProgrammeCsv.mustBeQuoted` at `b700b6e` | none | accepted |
| S17 | Low | With the database down, a request gives 500 with the text of a `CannotCreateTransactionException`, which `DatabaseFailureAdvice` does not catch. Boot 2.7 shows the class `SQLTransientConnectionException`, the pool name and the 30 s timeout; Boot 4 shows only "Could not open JDBC Connection for transaction". No host and no SQL. | `DatabaseFailureAdvice` | `892de97` | CLOSED on `892de97`: with the database down, 500 with the fixed message, health 503 |
| S18 | Medium | `GET /speakers/search?q=` is anonymous and in no spec. With an empty `q` it returns every speaker with name, email and bio in one request. | `SpeakerController` `/search` | Proposal: remove the endpoint, as `PUT /speakers` was removed (S1) | | OPEN on `7e03f8f` |
| S2 | Medium | A NUL character in a text field gives 500, and the answer shows the SQL. | `application.properties:14` | `6e37faf` | CLOSED on `b7f1b79`: 400, no SQL |
| S2b | Low | 400 answers name Java classes. | `application.properties:14` |  | OPEN on `b7f1b79` |
| S3 | Medium | No limit on the body size. A 120 MB body gives `OutOfMemoryError` at a 256 MB heap. | `RequestLogFilter.java:32` | `9157ed4`, `84a7fab` | CLOSED on `62eec68`: 413 above 1 MB, also chunked; no OOM with 80 parallel bodies of 1 MB |
| S4 | Medium | Tomcat 9.0.83 (Boot 2.7.18) has a multipart DoS (CVE-2025-48988). | `pom.xml:18` | `84a7fab` | CLOSED on `62eec68` |
| S5 | Medium | Anybody can unregister anybody from a workshop. | `WorkshopRegistrationController.java:42` | `0a9c234` | CLOSED on `b7f1b79`: 404, 404, 400, then 204 |
| S5b | Medium | The email that R10 asks for is public: `GET /workshops/{id}/registrations` lists every email, as the request's `Registration` schema requires. Anybody can read the list and then unregister an attendee. | `WorkshopRegistrationController` | `b8b5077` | CLOSED on `b8b5077`: the email alone 404, the right token 204, the token not in the list or the log |
| S5c | Low | The log mask for `token` allows only spaces, at most 8, before the colon. With a tab or 9 spaces, the token is in the log. | `RequestLogFilter` `TOKEN` at `b8b5077` | `e8ff81d` | CLOSED on `d14fda0`: no token in the log with a tab, a newline or 40 spaces; at most 0.025 s for 1 MB |
| S6 | Medium | A title can start a formula or a new row in `/admin/talks.csv`. | `Utils.java:73` | `9f3d7d2` | CLOSED on `b7f1b79` |
| S7 | Medium | Emails are in the container logs. | `RequestLogFilter.java:39`, `ConferenceService.java:76` | `9458d53`, `f210ff0` | CLOSED on `b7f1b79` |
| S8 | Low | The container runs as root. | `Dockerfile:19` | `4a8753f` | CLOSED on `b7f1b79` |
| S9 | Low | `/actuator/info` shows the Java, Boot and Spring versions. | `VersionInfoContributor.java:21` | Keep: the spring-boot-4 feature needs it | none | not a defect |
| S10 | Low | Anybody can decide on a talk, and the `speakerId` for a withdrawal is public. | `AcceptanceController.java:26` | The spec has no auth model: a PO question | none | not a defect |
| S13 | Low | `GET /talks` and `/admin/talks.csv` have no paging, so every request reads all talks. | `ConferenceService.listTalks` | Note only: paging would change the API that the specs and the harness use (The Hand, 2026-11-06). | none | not a defect |
| S10b | Low | Anybody can put talks on the programme and take them out: 15 DELETEs empty it. | `ProgrammeController` | The spec has no auth model: the same PO question as S10 | none | not a defect |
| S15 | Low | Co-speakers R12 is not access control: `actingSpeakerId` is optional, and the main speaker's ID is public in `GET /talks`. | `CoSpeakerController` (PR #9) | The spec makes the parameter optional, so that the harness keeps working: the same PO question as S10 | none | not a defect |
| S11 | High | The email mask in the request log takes time that grows with the square of the text length (ReDoS). An 80 KB body without `@` takes 16 s of CPU, and a stored long title costs the same on every list. Found in `9458d53` before the push. | `RequestLogFilter.java:67` at `9458d53` | `b7f1b79` | CLOSED on `b7f1b79`: 2 MB without `@` in 0.02 s |
| S12 | Medium | A talk title has no length limit. A long title is stored and comes back in every list and in the CSV. | `ConferenceService.submitTalk` | `d8612c3` | CLOSED on `d8612c3`: 200 code points 201, 201 code points 400, also with emoji |

## Log

| Date | Range | Result |
|------|-------|--------|
| 2026-11-06 | `d66b3df` (full review) | S1–S10 found. The exploit script finds S1–S8 OPEN on `9573d72`. |
| 2026-11-06 | `e532240` (early run) | S1 and S7 CLOSED. S11 found in `9458d53` (not pushed): a blocker. S12 found. S2, S2b, S4, S5, S6 and S8 OPEN. |
| 2026-11-06 | PR #5 `060ef9b` (Spring Boot 4.1.2, Java 25) | OK to merge. No hole reopened, none new: the same results as main (S1–S4, S6–S8 OPEN, S11 CLOSED). Only health and info under `/actuator`. Merge risks: `server.error.*` is now `spring.web.error.*`; the S3 limit must replace `ContentCachingRequestWrapper(request, Integer.MAX_VALUE)`; `jakarta.servlet`; keep the `USER` line. |
| 2026-11-06 | `e532240..b7f1b79` | S1, S2, S5, S6, S7, S8 and S11 CLOSED. S2b, S3 and S4 OPEN. Low: an email with more than 64 characters before the `@` passes `Utils.isValidEmail`, and the log mask leaves its start readable. |
| 2026-11-06 | push-candidate `c43bbaf..c4fe79c` | No blocker. S1, S2, S5, S6, S7, S8 and S11 CLOSED. S3 and S4 OPEN, accepted for this push. S2b OPEN. S5b found. |
| 2026-11-06 | `9bbbd83` (one ratings query for a talk list) | No blocker: one array parameter with IDs from the database. S13 noted. S5b fix with Frekken. |
| 2026-11-06 | `62eec68` (S3, S4) | S3 and S4 CLOSED. Only S2b (low) and S5b OPEN. |
| 2026-11-06 | `d8612c3` (S12) | S12 CLOSED. Only S2b (low) and S5b OPEN. |
| 2026-11-06 | `7d4d8e0` (scheduling) | No blocker. SQL bound; the slot lock and the unique indexes hold R8 under parallel PUTs; bad input gives 400; `schema.sql` safe on a restart with data. S10b noted. |
| 2026-11-06 | `b8b5077` (S5b) | S5b CLOSED. S11t: the token mask takes under 0.01 s on 600 KB of quotes and a 900 KB token. S5c (low) found. The exploit script is in `workflow/security/exploits.sh`. |
| 2026-11-06 | `f0f237e`, `9fa3cc4`, `e8ff81d` | No blocker. S5 and S5b stay CLOSED. S5c CLOSED. Only S2b (low) OPEN. |
| 2026-11-06 | `a242630`, `a697884`, `54f8f40` (push candidate `3393575`) | No blocker. 409 for an accepted talk that is not a workshop, no row saved; 404 for an unknown talk. Only S2b (low) OPEN. |
| 2026-11-06 | programme export (spec) | S14 accepted: no formula guard in `/programme.csv`. RFC 4180 quoting stays required. |
| 2026-11-06 | PR #9 `7560729` (co-speakers R11, R12) | OK to merge. The R11 migration deletes only orphan links, validates both keys, and is idempotent. R12 gives 403 before other checks. S15 noted. Condition: the merge must keep both new blocks in `schema.sql`. |
| 2026-11-06 | `b700b6e` (programme export) | No blocker. RFC 4180 quoting round-trips titles and speaker names; `text/csv` for every Accept header; read-only; 7 ms. S16 (low) found. |
| 2026-11-06 | `dc1a353` (GET hotfix, check-2) | No blocker (static review). `200 []` for a talk that is not a workshop, without reading registrations; 404 for an unknown talk; POST and DELETE unchanged. |
| 2026-11-06 | PR #9 `3ca19dd` (with main `7bdf99a` merged) | OK to merge. `schema.sql`: token ALTER, then the R11 block last. Migration from a `7bdf99a` database: only the orphans deleted, keys validated, the token hash and programme kept, the old token still works. Script: only S2b OPEN. |
| 2026-11-06 | origin/main `dfd57c9` (PR #9 merged) | Nothing reopened. Everything CLOSED except S2b (low), also S3b with `--big`. |
| 2026-11-06 | `05ac2c5` (scheduling hotfix, check-1) | No blocker (static review). Only 400 → 404 for a room or slot outside the list; the enum parse still comes first. |
| 2026-11-06 | `d4ef02a` (co-speakers hotfix, check-1 and check-2) | No blocker (static review). The R12 403 still comes first; the new 404 comes after it, on the locked talk; no SQL change; the limit of 2 untouched. |
| 2026-11-06 | PR #11 `76513a4` (ratings-cr: trimmed average, 2 decimals) | OK to merge. One constant query with MIN/MAX; no division by zero; edge cases (exactly 5, all equal, 1000 ratings) correct; script: only S2b OPEN, S4r CLOSED. |
| 2026-11-06 | PR #5 `815fd7a` (Spring Boot 4.1.2, with main `6227fcb`) | OK to merge. Boot 4 names, multipart off (S4r 415), only health and info, Jackson 3 and jakarta ports, cache limit kept, USER app. Script: only S2b OPEN. S17 (low) found, also on main. |
| 2026-11-06 | `267cb08`, `f95fd45` (submission-cap-cr) | OK. No speaker-then-talk lock path, so no deadlock cycle; R6 (10 at once, 3 rounds) gives exactly 1 success; 15 s of mixed load: no deadlock, no 500, nobody above 3. SQL bound. Script: only S2b OPEN. |
| 2026-11-06 | `892de97`, `2be6fff` | No blocker. S17 CLOSED (database stopped by hand: fixed 500 message). The talk limit stays 3. Only S2b OPEN. |
| 2026-11-06 | PR #8 `7e03f8f` (tech debt, merged before our review) | No hotfix needed. Nothing reopened; the advice, the NUL module and the body filter are still wired; SQL only in DAOs, bound; CSV guard kept. The empty-body 400 names method signatures (S2b class). S18 found (older than PR #8). |
