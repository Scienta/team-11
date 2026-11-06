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

### 2026-11-06: acceptance design review of `6841a88` (Frikke)

| # | Finding | Refactoring | Status |
|---|---------|-------------|--------|
| A1 | `AcceptanceService` throws `ResponseStatusException`: HTTP in the core (P2). | Replace Exception with a sealed result, mapped in `AcceptanceController` | done, `d41f940` |
| A2 | The service parses `Map<String, Object>` bodies (P3). | Introduce Parameter Object: the records `Decision` and `Withdrawal` with `parse` | done, `097763f` |
| A3 | `getTalk` is called only for its 404 and loads the ratings each time (command/query separation). | A lookup without ratings that returns `Optional` | done, `a78b665` |
| A4 | `TalkDto.status` is a `String`, and the code compares enum names (P4). | Replace Type Code with Enum | done, `09dd60d` (also in `CoSpeakerService`) |
| A5 | `TalkStatus` has a `switch` and `this ==` checks (P1). | Replace Conditional with enum constructor fields | done, `ed34210` |
| A6 | The R7 rule and `'SUBMITTED'` are SQL literals in `TalkDao` (P1). | Move Method `TalkStatus.countsTowardLimit()`, bound parameters | done, `effe5ad` |
| A7 | Two `listTalks` overloads with a flag argument. | Replace Flag Argument with `TalkListQuery`; Extract Class `TalkListing` (D1, P8) | open: waited for the co-speakers merge, which is now in |
| A8 | `refusal()` reads the talk again to find the reason. | Goes away with A1 | done, `d41f940` |
| A9 | Nested `Stream.of` and `flatMap` build the SQL arguments. | Extract Method | done, `db7dd57` |

### 2026-11-06: workshop-registration design review of `3121e67` (Frode)

| # | Finding | Refactoring | Status |
|---|---------|-------------|--------|
| W1 | `WorkshopTalk.status` is a `String` (P4). | Replace Type Code with Enum | done, `b6585fb` |
| W2 | The rule "only `ACCEPTED` takes registrations" is outside `TalkStatus` (P1). | Move Method `TalkStatus.takesRegistrations()` | done, `174d35f` |
| W3 | `refusal()` returns `Optional<RegistrationOutcome>`, so it can return `Registered`. | Extract Interface: a sealed `Refusal` subtype, also for `StatusChange` in acceptance | open (Frode, after Frikke) |
| W4 | The controller maps every outcome that it does not list to 404. | An explicit `NotAWorkshop` case; throw for the rest | open (Frikke) |
| W5 | `register()` mixes the lock, the counts, the rule and the insert. | Extract Method `registerIfTaken` | open (Frikke) |
| W6 | `Optional<List<Registration>>` means "not a workshop". | A sealed result: `Attendees` or `NotAWorkshop` (P2) | open (Frikke) |
| W7 | The DAO method `insert` returns domain data under a name for the mechanism. | Rename Method `addAttendee` | open (Frikke) |
| W8 | Two queries differ only in `FOR UPDATE`. | Extract Constant or Parameterize Method | open (Frikke) |
| W9 | The R5 tables have an unused "Seats" column. | Remove the column | done, `d029601` |

### 2026-11-06: batch `origin/main..17ba5c3` (21 commits, guardian)

No blocker. The 7 acceptance refactors change no status code. In a race, the 409 message from
`d41f940` can name the older reason.

| # | Finding | Refactoring | When | Status |
|---|---------|-------------|------|--------|
| G1 | `WorkshopTalk.status` is a `String`, but `TalkDto.status` is a `TalkStatus` (P4). The rule "takes registrations" is outside `TalkStatus` (P1). | Replace Type Code with Enum; Move Field into `TalkStatus` | tidy after | done: W1 `b6585fb`, W2 `174d35f` |
| G2 | `CoSpeakerService.UNCHANGEABLE_STATUSES` is a status rule outside `TalkStatus` (P1). | Move Field into `TalkStatus` | tidy after | Co-speakers code of Carl Kristian's team: the coordinator offers it in Slack. |
| G3 | `CoSpeakerService` throws `ResponseStatusException` (404, 409): HTTP in a service (P2). | Replace Exception with Sealed Result | tidy after | Same as G2. |
| G4 | `TalkStatusDao.moveArguments` and `TalkDao.insertIfBelow` both build an `IN (?, ?, …)` list and its arguments. | Extract Method into one helper, or bind one array with `status <> ALL(?)` | tidy after | Our queue, after the security fixes. |
| G5 | Three DAOs lock or read the talk row with their own SQL: `TalkStatusDao.lockStatus`, `TalkDao.findByIdForUpdate`, `WorkshopRegistrationDao.lockTalk`. | Consolidate into `TalkDao`: one locked read for each lock mode | tidy after | Our queue, after the security fixes. W8 covers the two workshop queries. |
| G6 | `WorkshopRegistrationService.register` nests three `Optional` lambdas: more than one level of abstraction. | Extract Method `registerIn(talk, request)` | tidy after | same as W5 (Frikke) |

Risk, closed: since `09dd60d`, `TalkDao` maps each stored status with `TalkStatus.valueOf`, so one
unknown value would give 500 on every talk read. Frode checked production before the change: 2582
talks, all with one of the 5 known values. The column is `NOT NULL` with the default `SUBMITTED`,
and only 2 places write it.

## Watch list

- Spring Boot 4 (Java 25, draft PR #5): a full architecture review before the merge. The new
  `Dockerfile` must keep `-XX:TieredStopAtLevel=1` in the `ENTRYPOINT`. At 0.15 CPU, start-up
  without the flag took 58.9 s, against the 60 s deploy gate.
- Merges from Carl Kristian's laptop: check only the fit with P1–P8 and what our features rely on.
  Do not repeat his team's feature review.
