# Change request: submission cap (`submission-cap-cr`)

Source: `request.md` in this folder (the change request from the harness, not edited). It changes the
limit of 3 talks in [Talks](../talks/spec.md) R7, [Acceptance](../acceptance/spec.md) R7 and
[Co-speakers](../co-speakers/spec.md) R7. Where it differs from an earlier answer of the product
owner, the change request overrides: it is the harness's spec.

## Rules

| ID | Rule |
|----|------|
| R1 | A speaker's limit of 3 talks counts every talk where they are the main speaker or a co-speaker. |
| R2 | A talk that is `REJECTED` or `WITHDRAWN` does not count, for the main speaker or the co-speakers. |
| R3 | Taking a co-speaker off a talk frees their place. A freed place can be used at once. |
| R4 | A speaker at the limit can't submit a new talk. |
| R5 | A speaker at the limit can't be added as a co-speaker to a talk that is still in the running (`SUBMITTED`, `WAITLISTED` or `ACCEPTED`). |
| R6 | The limit holds when submissions and co-speaker additions for one speaker arrive at the same time. |
| R7 | Nothing that exists is removed or changed. A speaker who is over the limit keeps every talk, but gets nothing new until they are below it. |
| R8 | A speaker's talk list still shows only the talks where they are the main speaker. |

## API

No new endpoints and no new fields. `POST /talks` and `PUT /talks/{id}/co-speakers/{speakerId}` get the
new rule.

## Our decisions

| Case | Decision | Why |
|------|----------|-----|
| A REJECTED talk (R2) | No longer counts | Change request overrides Acceptance R7, where a `REJECTED` talk counted (the product owner's earlier answer). |
| Co-speaking (R1) | Counts | Change request overrides Co-speakers R7 ("co-speaking doesn't count"). |
| What counts (R1, R2) | The talks in `SUBMITTED`, `WAITLISTED` or `ACCEPTED` where the speaker is the main speaker or a co-speaker. One talk counts once. | The main speaker can't also be a co-speaker of the same talk (Co-speakers R3). |
| `POST /talks` for a speaker at the limit (R4) | 409 `a speaker can submit at most 3 talks`, nothing is saved | Unchanged status and message of Talks R7: a conflict with the current state, and a place can be freed. |
| PUT of a co-speaker who is at the limit, on a talk in the running (R5) | 409 `this speaker already has 3 talks`, the talk does not change | A conflict with the speaker's current state: a rejection, a withdrawal or a removal frees a place. Harness pattern of today: a rule that the current state forbids is 409. |
| PUT of a co-speaker at the limit who is already a co-speaker of this talk | 200, talk unchanged | Co-speakers R5 ("changes nothing") comes first. The talk already counts for them. |
| The main speaker's own count | Plays no part when someone else is added as co-speaker | The limit is about the person who gets one more talk. |
| Order of the checks for PUT | as in Co-speakers, then the limit last: `actingSpeakerId` 400, talk 404, R12 403, speaker 404, R9 409, R3 400, R5 200, R2 409, then this R5 409 | A talk with two co-speakers is full for everyone, so its own 409 comes before the speaker's limit. |
| A speaker over the limit from before (R7) | Keeps everything. Each new talk or co-speaker place gets 409 while 3 or more count. | The request says so. The check is "3 or more", not "exactly 3". |
| A freed place (R3) | Free at once, in the same request flow: a withdrawal, a rejection or a `DELETE` of the co-speaker, then a new submission or PUT gets 201 or 200. | The request says "straight away". The count reads the current rows; nothing is cached. |
| Concurrency (R6) | `POST /talks` locks the speaker row (`FOR UPDATE`), counts and inserts in one transaction, as before. A co-speaker PUT locks the talk row, then the added speaker's row (`FOR UPDATE`), counts and inserts in one transaction. | Both requests for one speaker take turns on the speaker row. The lock order is always talk, then speaker, and `POST /talks` locks no existing talk, so no deadlock. A withdrawal or rejection only lowers a count, so it needs no speaker lock. |
| A speaker's talk list (R8) | Unchanged: main-speaker talks only | Co-speakers R6. |
| Adding a co-speaker to a `REJECTED` or `WITHDRAWN` talk | 409 `a withdrawn or rejected talk can't be changed`, as today (Co-speakers R9) | The change request says that the cap does not block it. The product owner, Stein, keeps R9 (2026-11-06 15:36:33, Slack #team-11, ts `1793975793.452099`): "It shall not be allowed to add co-authors [co-speakers] to talks that are rejected or withdrawn. This gives no value." User decision, 2026-11-06: follow the product owner. If a harness transcript shows this case and expects 200, raise it with the product owner, with the transcript. |

## Examples

Ada is a registered speaker. "Ada's talks" counts the talks where Ada is the main speaker or a
co-speaker.

### R1, R2, R4: submitting a talk

| Ada's talks before | New `POST /talks` by Ada | Status |
|--------------------|--------------------------|--------|
| 2 as main speaker | yes | 201 |
| 2 as main speaker, 1 as co-speaker | yes | 409 |
| 1 as main speaker, 2 as co-speaker | yes | 409 |
| 3 as main speaker, 1 of them `REJECTED` | yes | 201 |
| 3 as main speaker, 1 of them `WITHDRAWN` | yes | 201 |
| 2 as main speaker, 1 as co-speaker on a `REJECTED` or `WITHDRAWN` talk | yes | 201 |

### R1, R5: adding Ada as co-speaker

| Ada's talks before | PUT Ada on a talk that is | Status |
|--------------------|---------------------------|--------|
| 2 as main speaker | `SUBMITTED`, `WAITLISTED` or `ACCEPTED` | 200 |
| 3 as main speaker | `SUBMITTED`, `WAITLISTED` or `ACCEPTED` | 409 |
| 2 as main speaker, 1 as co-speaker | `SUBMITTED` | 409 |
| 3, one of them as co-speaker on this talk | `SUBMITTED` (Ada again) | 200 |
| 3 as main speaker, 1 of them `REJECTED` | `SUBMITTED` | 200 |
| 3 as main speaker | `REJECTED` or `WITHDRAWN` | 409 (Co-speakers R9, the product owner's answer) |

### R3: a freed place

| Given | Request | Status |
|-------|---------|--------|
| Ada: 2 as main speaker, 1 as co-speaker on talk T | `DELETE` Ada from T, then `POST /talks` by Ada | 200, then 201 |
| Ada: 3 as main speaker | withdraw one, then PUT Ada as co-speaker | 200, then 200 |

### R6: at the same time

| Given | Requests at the same time | Result |
|-------|---------------------------|--------|
| Ada: 2 as main speaker | 5 `POST /talks` by Ada and 5 PUTs of Ada on 5 different talks | exactly one 201 or 200, the other nine 409; Ada has 3 talks |

### R7: over the limit from before

| Given | Request | Status |
|-------|---------|--------|
| Ada: 3 as main speaker, 1 as co-speaker (allowed before) | `GET /talks/{id}` of each | 200, nothing removed |
| the same | `POST /talks` by Ada, or PUT Ada as co-speaker | 409 |

## Effect on existing features

- Talks R7: the count includes co-speaker places and leaves out `REJECTED` talks. Test: `TalksTest`
  (the R7 concurrency row is unchanged).
- Acceptance R7: a `REJECTED` third talk no longer counts. Test: `AcceptanceTest`
  `leavesWithdrawnTalkOutOfTheLimit` (the `REJECTED` row moves from 409 to 201). Hurl:
  `examples/our-acceptance.hurl` "the third talk is REJECTED, a fourth is 409" becomes 201.
- Co-speakers R7 is replaced. Tests: `CoSpeakersTest` `aSpeakerAtTheLimitCanStillCoSpeak` and
  `coSpeakingDoesNotCountTowardsTheLimit` change to the opposite. Hurl: `examples/our-co-speakers.hurl`
  "a speaker with 3 talks of their own can still co-speak, and co-speaking doesn't count".
- Scheduling: R6 setups that give a speaker 2 talks of their own and 2 co-speaker places now pass the
  limit. `SchedulingTest` still passes (81 tests); the Hurl setup for R6 (Ada) needs another speaker.
- Programme export (`b700b6e`, not on `origin/main` yet): `ProgrammeExportTest` (24) and
  `ProgrammeCsvTest` (18) pass with this change on a trial merge. The Hurl setup for R4 (Grace: 3
  talks of her own and 1 co-speaker place) needs another speaker.
- Until this change is deployed, a harness check for this change request fails, and a talks or
  co-speakers check that expects the old rule may pass. After the deploy, the opposite.

## Open questions for the product owner

- [x] Can a co-speaker be added to a rejected or withdrawn talk now? — No (Stein, 2026-11-06 15:36:33).
  Co-speakers R9 holds. See "Our decisions".
