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
| S1 | High | Anybody can change any speaker with `PUT /speakers/{id}`. It is in no spec. | `SpeakerController.java:41` | Remove (user, 2026-11-06) | | OPEN on `9573d72` |
| S2 | Medium | A NUL character in a text field gives 500, and the answer shows the SQL. | `application.properties:14` | Fix | | OPEN on `9573d72` |
| S2b | Low | 400 answers name Java classes. | `application.properties:14` | Fix with S2 | | OPEN on `9573d72` |
| S3 | Medium | No limit on the body size. A 120 MB body gives `OutOfMemoryError` at a 256 MB heap. | `RequestLogFilter.java:32` | Fix | | OPEN on `9573d72` |
| S4 | Medium | Tomcat 9.0.83 (Boot 2.7.18) has a multipart DoS (CVE-2025-48988). | `pom.xml:18` | Turn multipart off | | OPEN on `9573d72` |
| S5 | Medium | Anybody can unregister anybody from a workshop. | `WorkshopRegistrationController.java:42` | The email is needed: R10 in spec `9573d72`, a JSON body `{"email"}` without letter case; wrong or no email 404, no body 400 | | OPEN on `9573d72` |
| S6 | Medium | A title can start a formula or a new row in `/admin/talks.csv`. | `Utils.java:73` | Fix | | OPEN on `9573d72` |
| S7 | Medium | Emails are in the container logs. | `RequestLogFilter.java:39`, `ConferenceService.java:76` | Fix | | OPEN on `9573d72` |
| S8 | Low | The container runs as root. | `Dockerfile:19` | Fix | | OPEN on `9573d72` |
| S9 | Low | `/actuator/info` shows the Java, Boot and Spring versions. | `VersionInfoContributor.java:21` | Keep: the spring-boot-4 feature needs it | none | not a defect |
| S10 | Low | Anybody can decide on a talk, and the `speakerId` for a withdrawal is public. | `AcceptanceController.java:26` | The spec has no auth model: a PO question | none | not a defect |

## Log

| Date | Range | Result |
|------|-------|--------|
| 2026-11-06 | `d66b3df` (full review) | S1–S10 found. The exploit script finds S1–S8 OPEN on `9573d72`. |
