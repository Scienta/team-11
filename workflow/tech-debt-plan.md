# Technical debt plan for team-11 (CFP service, Java 17 / Spring Boot 2.7)

I read every file under `src/`, all the tests, `pom.xml`, the `Dockerfile`, both workflows, `rollout.sh`, the Hurl examples and the workflow docs. I changed nothing.

The inventory was written before the harness token was available, from the published Hurl examples. The **B** items have since been checked against the harness specs: see "Checked against the harness specs" below.

Paths are relative to `/Users/cke/gits/scienta/team-11`. `CS` means `src/main/java/no/scienta/cfp/ConferenceService.java`. Line numbers refer to `8cebff2`. `ConferenceService.java`, `Utils.java`, `SpeakerForm.java`, `pom.xml`, `Dockerfile` and `deploy.yml` have changed since, so look those lines up again.

---

## Status after `main` at `d3c9b88` (2026-11-06)

Since the plan was written, `main` has 19 new commits. These items are done:

| Item | Commit | What changed |
|---|---|---|
| B4 | `717cefa`, `8421601` | `POST` and `PUT /speakers` share `requireBioWithinLimit`: at most 500 characters, counted as code points. `@Size` is gone from `SpeakerForm`. |
| B5 (email) | `2b48135`, `ae70e66` | One rule, `Utils.EMAIL`, on both `POST` and `PUT`: `local@label.label`, no whitespace (Unicode whitespace included). Matches the product owner's answer (question 1). |
| Step 21, `usShort` part | `514f3b6` | `ratedAtDisplay` uses the explicit pattern `"M/d/yy, h:mm a"`, with a test. The JDK 20+ risk in section 4 is gone. |
| Section 4, Java 17 | `597c1f5` | `pom.xml`, `Dockerfile` and CI run Java 17. Records, switch expressions and text blocks are now available. |
| Reviewer IDs above 2147483647 | `9b66dc9` | `rating.reviewer_id` is now `BIGINT`, through an `ALTER TABLE` in `schema.sql` (Ratings R1). Not in the original inventory. |
| Specs | `d0ebbf9` and others | `workflow/specs/speakers` and `ratings` have `spec.md` with "Our decisions". `talks` has only `request.md`. |

Changes to the basis for the plan:

- **`schema.sql` is now the migration path.** `9b66dc9` changes a column with an `ALTER TABLE` that does nothing on later runs. That's allowed: in-place changes that are safe to run again are fine, but D20 and "Skip during the game" still apply (no Flyway, no foreign keys).
- **TableTest (`05631f5`, `fac091d`, `d3c9b88`)**: JUnit is 5.14, `@TableTest` is the team's format for example tables, and CI publishes the report on GitHub Pages for the product owners. The characterisation tests in Phase 0 are written as `@TableTest` wherever the case is a table.
- **New tests and Hurl files** (`RatingsTest`, more in `SpeakersTest` and `UtilsTest`, plus `examples/our-speakers.hurl` and `our-ratings.hurl`) cover part of D15. Steps 4 and 5 get smaller.

Still exactly as in the plan: B1 (SQL injection, `SpeakerDao.java:52`), D16 (tests after deploy with `continue-on-error`), B2 (`Utils.MAX_TALKS = 5`), B3 and B9 in `/admin/stats`, D1 (`ConferenceService` has grown), `isNumeric` and the `@Disabled` test (D9, D11).

---

## Checked against the harness specs (2026-11-06, tick 15)

Specs: `talks` V1, `speakers` V1, `ratings` V2. At tick 15 all 15 harness checks for the three base features were OK.

| Item | What the spec says | Conclusion |
|---|---|---|
| B2 | Talks: "A speaker may submit at most **3** talks", and the limit holds under concurrent submissions. | The code is right. `Utils.MAX_TALKS = 5` and the README are wrong: remove the constant and fix the README (steps 10 and 25). |
| B3 | Ratings: `averageRating` rounds halves up "wherever a talk is shown"; ranking uses the average "as shown, rounded". | Talk averages and ranking are right (`CS:199` compares the rounded value). `/admin/stats` is in no spec. **Product owner (via Stein, #team-11, 2026-11-06 12:59):** round halves up there too (4.25 becomes 4.3). Behavioural fix: `/admin/stats` `averageRating` switches from `Utils.roundRating` (floor) to the same HALF_UP rounding as talks, and `UtilsTest` changes with it. The method (an average of each talk's average) is confirmed under B9. |
| B4 | Speakers: "A bio is at most **500** characters." | **Fixed** in `717cefa` and `8421601`: `PUT` has the same 500 limit as `POST`. |
| B5 | Speakers: "A speaker needs a real name and a real email to register." | `POST /speakers` checks only `@NotBlank` on email. **Product owner (via Stein, #team-11, 2026-11-06 12:58):** for now an address shaped like `noe@noe.domene` (something@something.domain) is enough. Keep the rule in one place so it's easy to change later. **Fixed** in `2b48135` and `ae70e66` with one shared rule (`Utils.EMAIL`). Still open from B5: `PUT` accepts a name of only spaces (`name.equals("")` in `updateSpeaker`), while `POST` rejects it. |
| B6 | Not covered. | Our decision. |
| B7 | Talks: "nothing else is a format or a level". | 400 for `KEYNOTE` is right. Closed. |
| B8, B10, B11 | `/admin/*` and `/speakers/search` are in no spec. | Our decision. The harness does not check them. B1 (SQL injection) still stands as a security fix. |
| B9 | `/admin/stats` is in no spec. | **Product owner (via Stein, #team-11, 2026-11-06 13:00–13:01):** `averageRating` is the average of each talk's average, which is today's behaviour and stays. `totalHours`: "for now, whole hours", but store the data so it's easy to show decimals later. Today `totalHours` is computed from the stored minutes, so decimals stay easy to add. **Product owner (13:06):** round with standard mathematical rules, everywhere: 185 min gives 3 and 210 min gives 4, where today it truncates. Behavioural fix. Stein asked not to be asked about rounding again without a special reason. |
| B12 | The status codes for an unknown speaker are not specified. | Our decision. Write it in "Our decisions". |
| B13 | Ratings: "A score is a whole number from 1 to 5. Nothing else is a score." | Rejecting `3.0` and `"3"` is right. Closed. |

### Product-owner answers for the drip features

Recorded here until `workflow/specs/<feature>/spec.md` exists. Move them there under "Our decisions" / "Open questions".

| # | Question | Answer (Stein, #team-11) |
|---|---|---|
| 4 | acceptance: do withdrawn or rejected talks count towards the limit of 3? | 13:07: a withdrawn talk does **not** count; a rejected talk **does** count. So a speaker who withdraws a talk can submit a new one. |
| 5 | acceptance: rating withdrawn, rejected or accepted talks; are withdrawn talks still listed in `?sort=rating`? | 13:08: reviewers **cannot** rate a talk that is withdrawn, rejected or accepted, only `SUBMITTED` and `WAITLISTED` talks. Still open (5b, asked again 13:09): whether withdrawn talks stay in `GET /talks` and `?sort=rating`. |
| 6 | acceptance: may an accepted talk be withdrawn? | 13:09 (labelled "7", confirmed as 6 at 13:11): **yes**, that's intended. He'd also like a simple overview of gaps in the programme so they can be filled. That's a separate wish and not in any feature request. |
| 7 | co-speakers + acceptance: can a co-speaker withdraw a talk? | 13:11: **no**, only the main speaker can withdraw a talk. |
| 8 | co-speakers + acceptance: co-speakers on withdrawn, rejected or accepted talks | 13:11: co-speakers **can** be added to an accepted talk, but **not** to a rejected or withdrawn one. Still open (8b, asked 13:11): can co-speakers be removed from them? |

The specs also confirm these constraints from sections 1c and 4:

- **Trailing slash**: `/speakers/` and `/speakers/{id}/` "must keep working". Spring Boot 3 would break this unless we add a workaround.
- **`submittedAt`** must be `yyyy-MM-dd'T'HH:mm:ss.SSS+00:00`, never `Z`, and `submittedOn` must be `yyyy-MM-dd`. Switching the JSON to `java.time` types is ruled out.
- **`ratedAtDisplay`** must be `M/d/yy, h:mm AM` with "exactly one ordinary space" before AM/PM. Met on every JDK since `514f3b6`.
- **Email uniqueness ignores case**: already met (`schema.sql:47`, `SpeakerDao.java:32`).

---

## 1. Inventory

### 1a. Behavioural bugs and code/spec mismatches (separate `fix:` commits, each needs a person's decision)

| ID | Where | What | Severity |
|---|---|---|---|
| **B1** | `SpeakerDao.java:52` | **SQL injection.** It builds `"... WHERE name ILIKE '%" + name + "%'"` from `?q=`. `GET /speakers/search` has no authentication (the comment at `SpeakerController.java:51` calls it "admin only", but nothing enforces that). Because `server.error.include-message=always` (`application.properties:14`), the SQL error text also comes back in 500 responses. Breaks review rubric item 7. | **Critical** |
| **B2** | `CS:145,153,156`, `Utils.java:17`, `README.md` API table | **Talk limit: 3 or 5?** The code, its message, its javadoc and `TalksTest` all say 3. `Utils.MAX_TALKS = 5` is never used, and the README says "max 5". Check the talks spec. Remove the misleading constant only after that decision. | High |
| **B3** | `CS:223-227` vs `Utils.java:104-106`, `CS:261`, `UtilsTest.java:36-42` | **Rating rounding differs.** A talk's `averageRating` rounds HALF_UP (4.25 becomes 4.3). `/admin/stats` `averageRating` uses `roundRating`, which **floors** (4.25 becomes 4.2), and `UtilsTest` pins the floor behaviour (comment "CFP-61"). There's also a commented-out call to `roundRating` at `CS:226`. Check the ratings spec (R4). | High |
| **B4** | `SpeakerForm.java:17` vs `CS:94` | Bio limit is 500 when you create a speaker and 1000 when you update one. | Medium |
| **B5** | `SpeakerForm.java:14-15` vs `CS:91` | `POST /speakers` only checks that email is `@NotBlank`. `PUT` runs `Utils.isValidEmail`. Name on `PUT` is checked with `equals("")` (`CS:88`), so a name of `"   "` is accepted, but `@NotBlank` rejects it on `POST`. | Medium |
| **B6** | `CS:97-100` | `updateSpeaker` checks then writes (TOCTOU) and has no `DuplicateKeyException` catch. Two concurrent email changes return **500** instead of 409. `registerSpeaker` handles this case (`CS:64`). | Medium |
| **B7** | `CS:38`, `ConferenceServiceTest.java:38-43`, `README.md` | `KEYNOTE`: the README lists it, the code comment says keynotes are invited and not submitted, and the test pins a 400. Check the spec. | Medium |
| **B8** | `ReportController.java:34` vs `CS:244` | There's no foreign key on `talk.speaker_id` (`schema.sql:22`). One orphan talk makes `/admin/talks.csv` return **404** (because `getSpeaker` throws), while `/admin/stats` skips orphans. `Utils.csv` (`Utils.java:71-79`) replaces `"` with `'`, which isn't RFC 4180 quoting. | Low |
| **B9** | `CS:260-261` | `totalHours` uses integer division, so it truncates. The stats average is an average of per-talk averages, not of all scores. Is either specified? | Low |
| **B10** | `application.properties:14-15` | `include-message=always` puts internal exception text in 500 bodies. Changing it changes error bodies, so it's a person's call. | Medium (security) |
| **B11** | `SpeakerController.java:51-55`, `ReportController.java:23-28` | `/speakers/search` and `/admin/*` have no auth and expose emails. Probably leave it for the game, but write the decision down. | Low |
| **B12** | `CS:124-126` vs `CS:170-172` vs `CS:184` | Unknown speaker gives 400 on `POST /talks`, 404 on `/speakers/{id}/talks`, and `200 []` on `/talks?speakerId=`. Check each against the specs. | Low |
| **B13** | `CS:274` | `score` must be an `Integer` instance, so `3.0`, `"3"` and numbers above `Integer.MAX_VALUE` get 400 "whole number". Check this against the spec before anyone changes the binding (see R6). | Low |

### 1b. Structural debt (external behaviour unchanged)

| ID | File:line | Principle broken | Severity / risk |
|---|---|---|---|
| D1 | `CS:21-294` (the "TODO split this up (CFP-140)" note is at `CS:24`) | SRP / god class. One class holds speakers, talks, ratings and statistics, and all three controllers depend on it. Every drip feature lands here and conflicts with every other. | **High.** It's the main cost driver for new features. |
| D2 | `CS:31-39` and `CS:128-141` | DRY. Format durations are defined twice: in a map, and in a switch where `default: 45` silently covers PRESENTATION. Levels are a literal list at `CS:118`. Neither is a type (Effective Java items 34 and 36: use enums, not string constants). | High. A new format has to be added in two places. |
| D3 | `CS:116` | Hidden behaviour. The 400 message `"format must be one of " + DURATIONS.keySet()` depends on **`HashMap` iteration order**, and with `include-message=always` that order is part of the response body. Any refactor to an enum or `EnumMap` changes the text unless the order is pinned. | High *trap* for refactoring |
| D4 | `TalkDto.java:8-21` (the javadoc admits it's "used internally all over the place") | Mutable public fields, and one type used for both the domain and the API. `addRatings` (`CS:215-221`) mutates its argument, so it isn't pure. Uses `java.util.Date`/`java.sql.Date`. | Medium |
| D5 | `SpeakerDao.java:18-24,51-53`, `CS:57-106`, `SpeakerController.java` | Speakers are passed around as untyped `Map<String,Object>`, with `(String) existing.get("email")` casts (`CS:97,148`, `ReportController:40`). Effective Java item 64 (refer to objects by interfaces) is misapplied here: these should be proper value types. | Medium |
| D6 | `TalkForm.java:7-14` | Public mutable fields. The javadoc says validation lives in the service "because CFP-87". `SpeakerForm` uses Bean Validation instead, so there are two validation styles. | Medium |
| D7 | `CS:173-176, 184-187, 240-246`, `ReportController.java:33-34` | N+1 queries. One `ratingDao.stats` per talk on every list, plus one `speakerDao.findById` per talk in stats and CSV. With the pool capped at 5 and up to 80 concurrent requests, this is the likeliest latency problem under harness load. | Medium (perf) |
| D8 | `TalkDao.java:59` | Transaction boundary in the wrong layer. The DAO has its own `@Transactional`, nested inside the service's (`CS:110`). The `FOR UPDATE` row lock (`TalkDao:61`) is correct, but which code owns the lock is unclear. | Low |
| D9 | `Utils.java` (whole file) | A "Utils" grab-bag (SRP), using thread-unsafe-by-design `SimpleDateFormat` instead of `java.time`. It has no `Clock`, so time can't be tested, which is why `UtilsTest.java:44-50` is `@Disabled("flaky on Jenkins")`. `CS:284,291` formats a string, parses it back and formats it again. Dead `isNumeric` (`:89-99`). `capitalize` uses the default locale (`:112`). | Medium |
| D10 | `ReportController.java:28-51` | Controller builds the CSV itself, so presentation logic sits in the web layer (SRP). | Low |
| D11 | `CfpApplication.java:10-11`, `CS:38,226`, `RatingDao.java:33-35`, `TalkDao.java:70-75`, `TalkForm.java:13`, `SpeakerForm.java:20-21`, `NotificationService.java:36-43`, `Utils.java:115-117`, `pom.xml:58-63`, `application.properties:19`, `schema.sql:42-44` (leave schema as is) | Commented-out code and dead code. Clean code says delete it; git keeps history. | Low risk, cheap |
| D12 | `CS:67`, `NotificationService.java:33,44` | String-concatenated SLF4J messages, and emails (PII) logged at INFO. | Low |
| D13 | `NotificationService.java:16-20` | Field injection with `@Value`; Spring's guidance is constructor injection. The feature has been dead since 2022. | Low |
| D14 | Whole `no.scienta.cfp` package | Everything sits in one flat package; there's no package-by-feature, so each drip feature adds to one crowded package. | Medium (feature cost) |
| D15 | Tests | No test at all for ratings (`PUT /talks/{id}/ratings/{r}`), `GET /talks` sorting (`CS:180-213`), `GET /talks/{id}` 404, `PUT /speakers`, `/speakers/{id}/talks`, `/admin/*`, validation messages, or the error-body shape. `ConferenceServiceTest:46-50` stubs a value that is already the default. The HTTP helpers are copied three times (`SpeakersTest:75-92`, `TalksTest:84-89`, `HealthTest:110-116`). | **High.** Refactoring isn't safe without these tests. |
| D16 | `.github/workflows/deploy.yml:75-98` | Tests run *after* deploy with `continue-on-error: true`, so a broken refactor still deploys. The file's own comment says how to make them a gate. | **High** |
| D17 | `pom.xml:15-16,86` | Java release is declared three times. | Low |
| D18 | `Dockerfile:2,5,7` | Uses the image's `mvn` rather than `./mvnw`, so its Maven version can differ from the wrapper's 3.9.11. `-q` hides build diagnostics. Runs as root with no `USER`. No `-XX:MaxRAMPercentage`, so the JVM defaults to 25% of container memory. Base images aren't pinned to a digest. | Low |
| D19 | `README.md` (the part below `fagdag:end`) | Stale docs: Java 8, port 8081, `master`/Jenkins, `/votes`, `/talks/top`, "max 5". `examples/*.hurl` line 2 refers to `examples.hurl`, which doesn't exist. | Low |
| D20 | `schema.sql` | No migration tool and no foreign keys. Noted only: **don't change** (see Priorities). | — |

Checked and fine: no session `SET` anywhere, the pool is capped at 5 (`application.properties:8`), there are no advisory locks, and rating upserts use `ON CONFLICT` (`RatingDao.java:20-24`).

### 1c. Contract details a refactor can break without anyone noticing (build the tests around these)

- **Error bodies** come from Boot's `BasicErrorController` (`timestamp`, `status`, `error`, `message`, `path`, plus binding `errors` for `@Valid`). Keep `ResponseStatusException` with the same messages. Don't move to a `@ControllerAdvice` or `ProblemDetail`.
- **Date formats**: `submittedAt` (`java.util.Date`) serialises as `2026-11-06T12:00:00.000+00:00`, which `talks.hurl:34` checks with a regex. `submittedOn` (`java.sql.Date`) serialises as `yyyy-MM-dd`. Switching to `Instant`/`OffsetDateTime` would change this to `...Z`. Keep the serialised form.
- **JSON nulls**: `averageRating: null` must stay present in the body (`ratings.hurl:81`).
- **Binding lenience**: replacing `Map<String,Object>` in `rate` (`TalkController.java:44`) with a typed DTO would make Jackson coerce `3.0` and `"3"` to 3 and change 400s into 200s. Likewise, turning `TalkForm.format` into an enum changes the 400 message.
- **Trailing slash**: `GET /speakers/{id}/` returns 200 (`speakers.hurl:41-47`). This depends on Spring 5.3's default trailing-slash matching.

---

## 2. Ordered plan

Each step is one commit and passes `./mvnw package`. Write the cases as `@TableTest` where they're a table. Key: **S** = structural, **B** = behavioural (needs a person's decision), **T** = adds tests only.

### Phase 0: safety net (tests and CI only, no production code)

1. **T** `test: extract shared HTTP client for API tests`. Pull `request()`/`send()` out of `SpeakersTest`, `TalksTest` and `HealthTest` into one package-private `ApiClient`. Guarded by those three tests.
2. **T** `test: characterise talk submission and reads`. Exact body for each of the 4 formats (durations 10/20/45/120). Each 400 with its exact `message`, including the **exact `format must be one of [...]` string** (D3). Unknown speaker gives 400. 4th talk gives 409 with its message. `GET /talks/{id}` 404. Unknown JSON fields are ignored.
3. **T** `test: characterise talk listing and rating sort`. Covers `CS:180-213`: average descending, then count descending, then id ascending, unrated last; `sort=bogus` gives 400; the `speakerId` filter; unknown `speakerId` gives `[]`; `/speakers/{id}/talks` gives 404.
4. **T** `test: characterise ratings`. `RatingsTest` (`9b66dc9`, `fac091d`) already covers reviewer IDs (R1). Add whatever is missing from this list: upsert; score 0/6/`3.0`/`"3"`/missing; `reviewerId` 0; bad score on a missing talk gives **400, not 404** (validation order, `CS:270-283`); `ratedAtDisplay` regex; HALF_UP averages (scores 4,4,5 give 4.3).
5. **T** `test: characterise speaker update and validation`. `SpeakersTest` already covers the email rule and the bio limit on both `POST` and `PUT`. Add the rest: `PUT` with a name of only spaces, a 409 conflict, and a case-only change of the speaker's own email. `POST` binding-error body; bio of 501 characters gives 400; trailing-slash GET.
6. **T** `test: characterise admin stats and CSV`. Keys and order, `totalHours` truncation, floored `averageRating`, CSV header, a row with `;` and `"` in it, `Content-Type` and `Content-Disposition`.
7. **T** `test: pin error response body shape`. One test asserts the full JSON keys for a `ResponseStatusException` 404 and for a `@Valid` 400.
8. **S (CI)** `ci: run tests before deploy`. Move the `tests` job ahead of deploy, add `needs: tests` and drop `continue-on-error` (`deploy.yml:75-98`). Don't touch the env/secret lines (`:34-42`). A person approves this. Risk: `HealthTest` pauses containers, which could make the gate flaky on GitHub runners. If it does, keep the gate and fix the test; don't drop the gate.

### Phase 1: security fix (behavioural)

9. **B1** `fix(speakers): bind search term as a parameter`. Write the failing test first: `q=O'Brien` currently returns 500 and should return 200 with a list. Change the query to `ILIKE '%' || ? || '%'`. A person decides whether `%` and `_` in `q` should be escaped. That's a second, separate behaviour change; default to leaving them alone.

### Phase 2: structural changes that make features cheaper (do these early)

10. **S** `refactor: remove commented-out and dead code`. Covers D11, `Utils.isNumeric` and its test, and the `@Disabled` test (`UtilsTest:44-50`). **Leave `Utils.MAX_TALKS` alone until B2 is decided**, so the mismatch stays visible.
11. **S** `refactor: name the per-speaker talk limit`. One constant, `MAX_TALKS_PER_SPEAKER = 3`, used at `CS:145` and `CS:156`, with the message built from it. Same output. Guarded by step 2 and `TalksTest`.
12. **S** `refactor: introduce TalkFormat enum with durations`. Replaces `CS:31-39`, `CS:128-141` and `CS:115`. Keep `TalkForm.format` as a `String` and parse it in the service. Build the error message from an explicit list in the **order captured in step 2**. Guarded by steps 2 and 3 and `ConferenceServiceTest`.
13. **S** `refactor: introduce TalkLevel enum` (`CS:118`), the same way.
14. **S** `refactor: extract SpeakerService from ConferenceService`. `SpeakerController` and `ReportController` move to it.
15. **S** `refactor: extract RatingService`. Moves `rate`, `addRatings` and `calculateAverage`.
16. **S** `refactor: extract TalkService`. Moves submit/get/list.
17. **S** `refactor: extract ConferenceStatistics and TalkCsvExport`. Moves `statistics()` and the CSV body out of `ReportController` (D10). Then delete `ConferenceService` and split its test file to match.
   Steps 14 to 17 are guarded by Phase 0 plus `SpeakersTest`/`TalksTest`. Keep every `ResponseStatusException` message byte-for-byte.
18. **S** `refactor: replace speaker maps with an immutable Speaker type` (D5). A `record Speaker(long id, String name, String email, String bio)` (Java 17) with `@JsonPropertyOrder({"id","name","email","bio"})`. Guarded by steps 5 and 6.
19. **S** `refactor: split TalkDto into Talk and TalkResponse`. `Talk` and `TalkResponse` are records. `Talk` is immutable and comes from `TalkDao`. `TalkResponse` is built by a pure factory, `TalkResponse.of(Talk, RatingSummary)`, which removes the mutation at `CS:215`. Keep `Date`/`java.sql.Date` and the current property order. Guarded by steps 2, 3 and 6 plus the `talks.hurl` regexes.
20. **S** `refactor: move classes into feature packages`. Packages: `speaker`, `talk`, `rating`, `report`, `platform` (health, request log, version info). Pure moves, with the tests moved alongside. `RequestLogFilter.MAX_BODY_CHARS` is package-private, so its test moves with it.

### Phase 3: worth doing once the above is in

21. **S** `refactor: inject a Clock and use java.time for timestamps`. Split `Utils` into `Timestamps` (now/today via `Clock`), `DisplayFormats` (`usShort`, `norwegianDate`), `CsvField`, `EmailAddress` and `RatingAverage`. `usShort` already has the explicit pattern (`514f3b6`). Prove the output is identical to today's with a test pinned to a fixed `Clock`. Restore the "now" test using the fixed clock. Remove the format/parse/format round trip at `CS:284-291`.
22. **S** `perf: load rating summaries in one query` (D7). Add `SELECT talk_id, COUNT(*), COALESCE(SUM(score),0) ... GROUP BY talk_id` for lists, and join speaker names for the CSV. Guarded by steps 3 and 6, which need several talks with ratings. This cuts connection hold time with the pool at 5.
23. **S** `refactor: keep the transaction boundary in the service`. Remove `@Transactional` from `TalkDao.java:59`; the service already opens the transaction at `CS:110`. Guarded by `TalksTest.concurrentSubmissionsNeverExceedThreeTalks`.
24. **S** `refactor: use parameterised logging without emails` (D12) and `refactor: constructor-inject NotificationService` (D13). These change log output only, not the HTTP contract.
25. **S** `docs: update README for the current service` (D19). Edit only below `fagdag:end`, and fix the Hurl header comments. Fill in the talk limit once B2 is settled.
26. **S** `build: declare the Java release once` (D17). Since `597c1f5` the compiler plugin reads `${maven.compiler.release}`, but `java.version` and `maven.compiler.release` are both still declared.

### Behavioural queue (one `fix:` commit per item, after a person decides, each with a failing test first)

B4 and B5 (email) are done. Ready to build: B2 (the spec says 3), B3 and B9 (decided by the product owner: round halves up in `/admin/stats` too, and round `totalHours` normally), and the name check from B5. Then B6, B8 and B10–B12 once we've decided them. Put each decision in `workflow/specs/<base>/spec.md` "Our decisions" (rules file) and cite the rule IDs in commit messages.

---

## 3. Priorities given the drip features

**Do first (they make every later feature cheaper or safer):**
- Steps 1 to 8: without the characterisation tests and the CI gate, nothing else is safe, and the tests are reusable as the spec example tables.
- Step 9 (B1), since it's a live injection on a public URL.
- Steps 12 and 13, so a new format or level is one enum constant.
- Steps 14 to 17, so each drip feature gets its own service instead of growing the god class and colliding with the others.
- Step 20 (feature packages).

Do 14 to 20 in a lull between drips: each is a few minutes, but they touch every file.

**Next, when there's time:**
- Steps 18 and 19 (typed models). These matter most if drip features add fields to talks or speakers.
- Step 22, if the harness or the request log shows slow list endpoints. Watch the `in N ms` values in `RequestLogFilter` lines.
- Step 21, if a drip feature involves time (deadlines, schedules).

**Low value, do last:** steps 10, 11 and 23 to 26, and the Dockerfile items (non-root `USER`, `./mvnw` in the image, `MaxRAMPercentage`). Each Dockerfile change is a deploy risk for tiny gain, so batch them in one commit late in the day or skip them.

**Skip during the game (risk is greater than the benefit):**
- **Flyway or Liquibase**: needs a baseline over a live database that keeps its data.
- **Adding foreign keys or touching `vote`/old columns**: existing orphan rows would make start-up `schema.sql` fail, and a failed start-up means a failed deploy. If ever needed, do it expand → migrate → contract.
- **Global `@ControllerAdvice` or ProblemDetail**: changes error bodies.
- **Enum-typed or DTO-typed request bodies for `format`/`level`/`score`**: these change 400 vs 200 behaviour and the messages (1c).
- **`java.time` types in the JSON**: changes date strings.
- **Deleting `NotificationService`**: not worth it.
- **`@Transactional(readOnly=true)` everywhere**: negligible gain.

---

## 4. Version upgrades (optional, keep separate)

**Done in `597c1f5`: the service runs Java 17 on Boot 2.7.18.** The original assessment is kept below. The TableTest report plugin needs JDK 21, so CI runs it on 21 after the tests (on 17).

**Java 11 to 17, staying on Boot 2.7.18: worth it, but only after Phase 0, in its own commits, during a quiet period.** Boot 2.7.18 runs on Java 17 (and up to 21). The gains apply directly here: records (steps 18 and 19 shrink to a few lines and match the immutability rule), switch expressions, pattern matching `instanceof` (`CS:274`), `Stream.toList()` and text blocks for the SQL.

Changes needed:
- `pom.xml:15-16,86`
- `Dockerfile:2,10` (`maven:3.9-eclipse-temurin-17`, `eclipse-temurin:17-jre`)
- `deploy.yml:88` (`java-version: "17"`)

Deploy-contract risks:
- The image build. The rollout rolls back automatically if `/health` doesn't return the new SHA within 60 s, but the push still costs a deploy.
- Locale data. CLDR changes `DateFormat` output between JDKs. On 17 the US SHORT format is unchanged.

**Fixed for `ratedAtDisplay` in `514f3b6`; `norwegianDate` still uses the locale.** Original warning: **Do not go to Java 21 (or anything from 20 up) without step 21 first.** From JDK 20, CLDR 42 puts a narrow no-break space (U+202F) before "PM". `ratedAtDisplay` (`Utils.java:53-57`) would change, and `ratings.hurl:46` (and probably the harness) expects a normal space. `norwegianDate` could also change. The explicit pattern in step 21 removes this risk.

**Spring Boot 2.7 to 3.x: not during the game.** It needs Java 17, and it breaks things:
- `javax.*` to `jakarta.*` (`RequestLogFilter.java:6-9`, `SpeakerController.java:6`, `SpeakerForm.java:3-4`, `RequestLogFilterTest.java:10-12`).
- `ResponseStatusException.getStatus()` becomes `getStatusCode()` (`ConferenceServiceTest`).
- Most importantly, **Spring 6 turns trailing-slash matching off by default**, so `GET /speakers/{id}/` returns 404 and Speakers R7 (`speakers.hurl:41-47`) fails. That's a direct contract break unless you add an explicit workaround.
- The error-body and Jackson defaults would need re-checking against the step 7 test.

Boot 2.7 has been out of open-source support since November 2023, which matters for a long-lived service but not for a one-day game. If you ever do it, do it after the game, behind the Phase 0 tests plus a Hurl run against a staging container.

---

### Critical files for implementation
- /Users/cke/gits/scienta/team-11/src/main/java/no/scienta/cfp/ConferenceService.java
- /Users/cke/gits/scienta/team-11/src/main/java/no/scienta/cfp/SpeakerDao.java
- /Users/cke/gits/scienta/team-11/src/main/java/no/scienta/cfp/TalkDto.java
- /Users/cke/gits/scienta/team-11/src/main/java/no/scienta/cfp/Utils.java
- /Users/cke/gits/scienta/team-11/.github/workflows/deploy.yml
- /Users/cke/gits/scienta/team-11/src/test/java/no/scienta/cfp/TalksTest.java (the pattern to follow for the new characterisation tests)
