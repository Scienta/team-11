# Architecture review

The architecture guardian keeps this file. The guardian reviews each batch of commits before a push
(`workflow/review-rubric.md` item 8). The guardian does not implement code.

## Patterns we follow

Check a new feature against each pattern. A change that breaks a pattern needs a reason in its
commit message.

| ID | Pattern | Example |
|----|---------|---------|
| P1 | A rule about a talk status is a field of the `TalkStatus` enum, not a set or a `switch` somewhere else. | `reachableFrom`, `reviewersNeeded`, `openForRating`, `countsTowardLimit` |
| P2 | A service returns an expected outcome as a sealed interface of records. The controller maps each record to an HTTP status. A service throws no `ResponseStatusException`. | `StatusChange`, `RegistrationOutcome`, `UnregistrationOutcome` |
| P3 | The controller parses a request body into a record with a static `parse` that returns `Optional`. Only `parse` creates the record, so each instance is valid. | `Decision`, `Withdrawal`, `RegistrationRequest` |
| P4 | A status is a `TalkStatus` everywhere in Java. The DAO maps the text column once. | `TalkDto.status`, `WorkshopTalk.status` |
| P5 | A rule is a pure function of data that the service reads first. The database, the clock and the log stay at the edges. | `WorkshopTalk.refusal(seatsTaken, emailRegistered)` |
| P6 | A limit or a uniqueness rule holds under concurrent requests: one conditional `UPDATE`, a row lock (`FOR UPDATE`, `FOR SHARE`) in a `@Transactional` method, or a unique index. No advisory locks. | `TalkStatusDao.moveTo`, `TalkDao.insertIfBelow`, `WorkshopRegistrationDao.insert` |
| P7 | Each class has a JavaDoc that states its responsibility and the rule IDs it implements. | all classes above |
| P8 | A new feature gets its own service. `ConferenceService` does not grow (tech debt D1). | `AcceptanceService`, `WorkshopRegistrationService` |

Java 17 has no pattern matching for `switch`, so a controller maps a sealed outcome with an
`instanceof` chain. After the upgrade to Java 25 (Spring Boot 4), use a `switch` with record patterns.

## Log

The SHAs in a section are the ones that the section reviewed. A later rebase can change them: look a commit up
by its subject with `git log --grep`.

### 2026-11-06: acceptance design review of `6841a88` (Frikke)

| # | Finding | Refactoring | Status |
|---|---------|-------------|--------|
| A1 | `AcceptanceService` throws `ResponseStatusException`: HTTP in the core (P2). | Replace Exception with a sealed result, mapped in `AcceptanceController` | done, `4d03267` |
| A2 | The service parses `Map<String, Object>` bodies (P3). | Introduce Parameter Object: the records `Decision` and `Withdrawal` with `parse` | done, `6456c84` |
| A3 | `getTalk` is called only for its 404 and loads the ratings each time (command/query separation). | A lookup without ratings that returns `Optional` | done, `b778566` |
| A4 | `TalkDto.status` is a `String`, and the code compares enum names (P4). | Replace Type Code with Enum | done, `a030b26` (also in `CoSpeakerService`) |
| A5 | `TalkStatus` has a `switch` and `this ==` checks (P1). | Replace Conditional with enum constructor fields | done, `5fba392` |
| A6 | The R7 rule and `'SUBMITTED'` are SQL literals in `TalkDao` (P1). | Move Method `TalkStatus.countsTowardLimit()`, bound parameters | done, `aa01d71` |
| A7 | Two `listTalks` overloads with a flag argument. | Replace Flag Argument with `TalkListQuery`; Extract Class `TalkListing` (D1, P8) | done: `0380244`, `3df81ef`, `6bef5fa` (Frode; not on `origin/main` yet) |
| A8 | `refusal()` reads the talk again to find the reason. | Goes away with A1 | done, `4d03267` |
| A9 | Nested `Stream.of` and `flatMap` build the SQL arguments. | Extract Method | done, `2039ed6` |

### 2026-11-06: workshop-registration design review of `3121e67` (Frode)

| # | Finding | Refactoring | Status |
|---|---------|-------------|--------|
| W1 | `WorkshopTalk.status` is a `String` (P4). | Replace Type Code with Enum | done, `898c422` |
| W2 | The rule "only `ACCEPTED` takes registrations" is outside `TalkStatus` (P1). | Move Method `TalkStatus.takesRegistrations()` | done, `334c8cc` |
| W3 | `refusal()` returns `Optional<RegistrationOutcome>`, so it can return `Registered`. | Extract Interface: a sealed `Refusal` subtype, also for `StatusChange` in acceptance | half done: the sealed `Refusal` subtypes are in (`27c49c8`, `7e284b1`), but `WorkshopTalk.refusal` still returns `Optional<RegistrationOutcome>`. Next: Change Signature to `Optional<RegistrationOutcome.Refusal>` |
| W4 | The controller maps every outcome that it does not list to 404. | An explicit `NotAWorkshop` case; throw for the rest | done, `0f54a97` |
| W5 | `register()` mixes the lock, the counts, the rule and the insert. | Extract Method `registerIfTaken` | done, `0322902` |
| W6 | `Optional<List<Registration>>` means "not a workshop". | A sealed result: `Attendees` or `NotAWorkshop` (P2) | done, `39a414d` (`AttendeesOutcome`) |
| W7 | The DAO method `insert` returns domain data under a name for the mechanism. | Rename Method `addAttendee` | done, `30170e1` |
| W8 | Two queries differ only in `FOR UPDATE`. | Extract Constant or Parameterize Method | done, `b3176b6` and `753a7a2` |
| W9 | The R5 tables have an unused "Seats" column. | Remove the column | done, `f348990` |

### 2026-11-06: batch `origin/main..17ba5c3` (21 commits, guardian)

No blocker. The 7 acceptance refactors change no status code. In a race, the 409 message from
`4d03267` can name the older reason.

| # | Finding | Refactoring | When | Status |
|---|---------|-------------|------|--------|
| G1 | `WorkshopTalk.status` is a `String`, but `TalkDto.status` is a `TalkStatus` (P4). The rule "takes registrations" is outside `TalkStatus` (P1). | Replace Type Code with Enum; Move Field into `TalkStatus` | tidy after | done: W1 `898c422`, W2 `334c8cc` |
| G2 | `CoSpeakerService.UNCHANGEABLE_STATUSES` is a status rule outside `TalkStatus` (P1). | Move Field into `TalkStatus` | tidy after | done in PR #9, `4beca2a` (`TalkStatus.isOpenForCoSpeakerChanges`) |
| G3 | `CoSpeakerService` throws `ResponseStatusException` (404, 409): HTTP in a service (P2). | Replace Exception with Sealed Result | tidy after | done in PR #9, `326a196` (sealed `CoSpeakerChange`) |
| G4 | `TalkStatusDao.moveArguments` and `TalkDao.insertIfBelow` both build an `IN (?, ?, …)` list and its arguments. | Extract Method into one helper, or bind one array with `status <> ALL(?)` | tidy after | done, `95de0b5`: `status = ANY(?)` and `<> ALL(?)` with one `String[]` |
| G5 | Three DAOs lock or read the talk row with their own SQL: `TalkStatusDao.lockStatus`, `TalkDao.findByIdForUpdate`, `WorkshopRegistrationDao.lockTalk`. | Consolidate into `TalkDao`: one locked read for each lock mode | tidy after | done, `753a7a2`: `TalkDao.lock(id, Lock.SHARE \| Lock.UPDATE)` |
| G6 | `WorkshopRegistrationService.register` nests three `Optional` lambdas: more than one level of abstraction. | Extract Method `registerIn(talk, request)` | tidy after | done with W5, `0322902` |

Risk, closed: since `a030b26`, `TalkDao` maps each stored status with `TalkStatus.valueOf`, so one
unknown value would give 500 on every talk read. Frode checked production before the change: 2582
talks, all with one of the 5 known values. The column is `NOT NULL` with the default `SUBMITTED`,
and only 2 places write it.

### 2026-11-06: push candidate `1613ca9..c4fe79c` (guardian)

No blocker. The candidate reports 391 tests and 0 failures (`c4fe79c`). The guardian did not run the suite again.

| # | Finding | Refactoring | When | Status |
|---|---------|-------------|------|--------|
| G7 | `AcceptanceService.decide` and `withdraw` took up to 4 pool connections one after another. The GET paths took 1 + N connections, with N the number of talks. | One `@Transactional` for each status change (`a871d15`); `@Transactional(readOnly = true)` on the GET paths (`4ffa97b`) | before the push | done |
| G8 | `TalkDao.lock` locks the row and also returns whether the talk exists: a command that returns a query result. | none: acceptable for a lock | — | accepted |
| G9 | The N queries for the ratings in `GET /talks` stay inside the read-only transaction. | Replace the loop with one query that joins the ratings | tidy after | done, `88e5d86`: one grouped query with `talk_id = ANY(?)` |

### 2026-11-06: Spring Boot 4, PR #5 at `060ef9b` (guardian)

OK to merge. A scratch merge with our local `main` on Java 25 found 7 fixes for the merge. After fixes 1 and 3–5, 431 tests ran and 1 failed: the row that fix 2 changes.

1. `Dockerfile`: keep `eclipse-temurin:25-jre`, `RUN useradd --system app`, `USER app` and `-XX:TieredStopAtLevel=1`.
2. `UpgradeContractTest`: the row `PUT /speakers/{id}/` becomes 405 (`59f45c9` removed the endpoint), also in the PR's `spec.md`.
3. `DatabaseFailureAdvice`: `javax.servlet` becomes `jakarta.servlet`.
4. `TextWithoutNulModule`: Jackson 2 becomes Jackson 3 (`tools.jackson…`, `deser.jdk.StringDeserializer`, no `IOException`).
5. `ErrorAnswersTest`, `WorkshopRegistrationTest`: `com.fasterxml.jackson.databind` becomes `tools.jackson.databind`.
6. `docs/specs/index.md`: make the report again.
7. `RequestLogFilter`: keep our `MAX_CACHED_BODY_BYTES` limit, not the PR's `Integer.MAX_VALUE`.

Start-up of the merged image at 0.15 CPU: 21.7 s with the JIT flag, 56.9 s without it. Keep the flag.

### 2026-11-06: later batches (guardian)

No blocker in any of them.

- `afdd22a` (413 above 1 MB), `c2f50a2` (multipart off), `2b9bc6f` (email lengths): fit P1–P8.
- `07f7d81` scheduling: fits P1, P3–P8. Locks in one order only: the slot `FOR UPDATE`, then the talk
  `FOR SHARE`.
- `b7ef1a1` cancellation token: only the SHA-256 hash is stored, and the comparison takes constant time.

| # | Finding | Refactoring | When | Status |
|---|---------|-------------|------|--------|
| G10 | `ProgrammeService.takeOut` returns a `boolean` (P2). | Replace the boolean with a sealed `TakeOutOutcome` | tidy after | done, `7791c00` |
| G11 | `scheduleIfAllowed` hides the insert in a `\|\|` inside a ternary. | Extract Method `addUnlessThere` | tidy after | done, `a223c14` |
| G12 | The literal `"WORKSHOP"` is in `Slot.fits`, `WorkshopTalk.isWorkshop` and `ConferenceService.DURATIONS`. | Replace Type Code with Enum: a `TalkFormat` with the duration and the long-slot rule | tidy after | open |
| G13 | The `TalkStatus` constructor takes 6 positional parameters, 4 of them booleans. | Introduce Parameter Object, or a set of capabilities for each status | tidy after | open, worse after PR #9: 7 parameters, 5 of them booleans. Fix it before the next rule field. |
| G14 | The controller builds `new CancellationToken(token)` from any string (P3). | Introduce Factory Method `CancellationToken.parse(body)` | tidy after | done, `f0f237e` |
| G15 | `registerIfTaken` makes and hashes a token also for a refused request. | Move Statements into the branch that saves | tidy after | done, `9fa3cc4` |
| G16 | `RequestBodyLimitFilter.ReadBodyRequest.getReader` calls `Charset.forName` on the client's charset: 500, not 400, for an unknown charset. No endpoint reads `getReader` today. | Catch the unknown charset and answer 400 | tidy after | open |
| G17 | `addUnlessThere` takes an `Optional<Place>` parameter. | Replace the parameter with a `boolean alreadyThere` | tidy after | open, small |

For the Spring Boot 4 merge (PR #5), add to the 7 merge fixes above:

8. `RequestBodyLimitFilter`: `javax.servlet` becomes `jakarta.servlet`.
9. The `UrlHandlerFilter` bean has no `@Order`, so its order against `RequestBodyLimitFilter` (both at
   `LOWEST_PRECEDENCE`) is not defined. Give it an explicit, higher precedence.

### 2026-11-06: co-speakers PR #9 at `7560729` (guardian)

OK to merge. A scratch merge onto our `3393575` conflicted only in `schema.sql` (keep both blocks)
and the generated `docs/status.md`. The full suite was green on Java 17: 545 tests, 0 failures.

- Closes G2 (`4beca2a`) and G3 (`326a196`). R12 reads `actingSpeakerId` at the controller edge.
- R11 adds cascading foreign keys to `talk_co_speaker` (`d9e5864`), against D20. A person accepted
  R11, and D20 in `workflow/tech-debt-plan.md` now names the exception.

| # | Finding | Refactoring | When | Status |
|---|---------|-------------|------|--------|
| G18 | `CoSpeakerService.refusal` returns `Optional<CoSpeakerChange>`, so it can return `Changed`. The same as W3. | Change Signature to `Optional<CoSpeakerChange.Refusal>` | tidy after | open |

### 2026-11-06: A7, the talk list out of `ConferenceService` (guardian)

No blocker. `0380244` (Introduce Parameter Object `TalkListQuery`), `3df81ef` (Extract Class
`TalkRatings`), `6bef5fa` (Extract Class `TalkListing`). `ConferenceService` has 252 lines (D1).
Frode showed byte-equal answers for 16 requests.

| # | Finding | Refactoring | When | Status |
|---|---------|-------------|------|--------|
| G19 | `TalkListing`, a service, throws a 400 `ResponseStatusException` for an unknown sort, and `TalkListQuery.sort` is a `String` (P2, P3). | Replace Type Code with Enum `TalkOrder`, parsed by `TalkListQuery.parse` in the controller | tidy after | open |
| G20 | The ranking in `TalkListing.orderedTalks` is a `for` loop with a hand-written comparator. | Replace Loop with Pipeline: `partitioningBy` and `Comparator.comparing(…).thenComparing(…)` | tidy after | open |
| G21 | `TalkRatings.addTo` changes each `TalkDto`. | Make `TalkDto` a record (P5) | later | open |

### 2026-11-06: programme export `b700b6e` (guardian)

No blocker. `Programme` and `ProgrammeCsv` are pure; one read-only query. `Utils.csv` and
`ProgrammeCsv` are two CSV writers on purpose: different readers, different rules.

| # | Finding | Refactoring | When | Status |
|---|---------|-------------|------|--------|
| G22 | `Programme` orders the rows by the declaration order of `Slot`. Only a JavaDoc line guards that order. | Compare by the slot's start time | tidy after | open |
| G23 | `ProgrammeRow.format` is a `String`. | Part of G12 (`TalkFormat`) | tidy after | open |

### 2026-11-06: hotfixes for harness checks (guardian)

No blocker in any of them. Each one first split a result that meant two things.

- `a242630` workshop POST: `RegistrationOutcome` splits `NoSuchTalk` (404) from `NotAWorkshop` (409).
- `dc1a353` workshop GET: `AttendeesOutcome` splits `NoSuchTalk` (404) from `NotAWorkshop` (200 `[]`).
  Open, small: spec row 59 (DELETE on a non-workshop) still gives "Same as GET" as its reason.
- `05ac2c5` scheduling: an unknown room or slot gives 404 on PUT, GET and DELETE. A missing or
  malformed body on PUT still gets Spring's 400 first, as the spec says.

Lesson: before a status change, check that the result type does not hold two cases with different
answers.

### 2026-11-06: report workflow `0239da4` (guardian)

No blocker. A push still deploys once from `deploy.yml`; only the `publish` job in `report.yml` has
`pages: write` and `id-token: write`. Merge fix 10 for Spring Boot 4: `report.yml` also needs Java 25.

## Watch list

- Spring Boot 4 (PR #5): after the merge, check that the 10 merge fixes above are in, the JIT flag
  first of all.
- Merges from Carl Kristian's laptop: check only the fit with P1–P8 and what our features rely on.
  Do not repeat his team's feature review.
