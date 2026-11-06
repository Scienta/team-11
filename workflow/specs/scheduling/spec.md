# Programme scheduling (`scheduling`)

Source: `request.md` in this folder (the feature request from the harness, not edited).

## Rules

| ID | Rule |
|----|------|
| R1 | The venue is fixed: rooms `R1`, `R2` and `R3`, each with the same five slots. Nobody can add or change a room or a slot. The times are `HH:mm` on a 24-hour clock. |
| R2 | The committee puts a talk in a room and slot, looks up what is in a room and slot, and takes the talk out again. A talk that was taken out can go somewhere else. |
| R3 | Only an `ACCEPTED` talk ([Acceptance](../acceptance/spec.md) R1) goes on the programme. |
| R4 | A talk must fit its slot: a `WORKSHOP` needs the long slot `S4`. A talk of any other format fits in every slot. |
| R5 | One talk per room and slot, and a talk is on the programme only once. To move a talk, take it out first. |
| R6 | Nobody speaks in two rooms at the same time: the main speakers and co-speakers ([Co-speakers](../co-speakers/spec.md)) of the talks in one slot are all different people. |
| R7 | The rules are checked when a talk is put on the programme. When the talk changes afterwards, the programme stays as it is. |
| R8 | Several committee members work on the programme at the same time, and the programme never breaks R4–R6. |

The slots, the same in every room:

| Slot | Start | End |
|------|-------|-------|
| `S1` | 09:00 | 10:00 |
| `S2` | 10:15 | 11:15 |
| `S3` | 11:30 | 12:30 |
| `S4` | 13:30 | 15:30 |
| `S5` | 15:45 | 16:45 |

No two slots overlap, so "at the same time" in R6 means "in the same slot".

## API

| Method | Path | Request body | Success status | Response body |
|--------|------|--------------|----------------|---------------|
| PUT | `/programme/{room}/{slot}` | `{"talkId": <integer>}` | 200 | Entry |
| GET | `/programme/{room}/{slot}` | | 200 | Entry |
| DELETE | `/programme/{room}/{slot}` | | 204 | none |

`{room}` is `R1`, `R2` or `R3`. `{slot}` is `S1` to `S5`. An Entry is
`{room, slot, start, end, talkId}`, for example
`{"room": "R2", "slot": "S4", "start": "13:30", "end": "15:30", "talkId": 7}`.

## Our decisions

| Case | Decision | Why |
|------|----------|-----|
| Room or slot that is not in the list, also in lower case (`R4`, `S6`, `r1`, `s1`), on PUT, GET and DELETE | 400 | The request gives the path parameters as a fixed list. A value outside the list is a malformed request, like a talk ID that is not a number. Capitals only, as for the talk formats (Talks R5). |
| Missing body or malformed JSON on PUT | 400 | Same as the base features |
| `talkId` missing, `null`, or not a JSON integer (`"5"`, `5.0`) | 400 | Same strictness as the score in Ratings R2 |
| `talkId` of a talk that does not exist, also 0 or negative | 400 | The ID is a field in the body, not a resource in the path, so it is a bad request, not a 404. Same as an unknown `speakerId` on `POST /talks` (Talks). |
| Other fields in the PUT body | Ignored | The request names only `talkId`. |
| Talk that is `SUBMITTED`, `WAITLISTED`, `REJECTED` or `WITHDRAWN` (R3) | 409, nothing is saved | A conflict with the talk's current state, as in Acceptance R8 and workshop registration R3. |
| `WORKSHOP` in `S1`, `S2`, `S3` or `S5` (R4) | 409, nothing is saved | The rule depends on the talk's format, which the request cannot change. We use 409 for every rule of the programme, so that a client sees one kind of "not allowed". |
| Another talk is already in the room and slot (R5) | 409, the entry does not change | A conflict with the current programme. Take the other talk out first. |
| The same talk in the same room and slot again | 200, the entry does not change | A double-click must not fail. PUT puts the programme in the state the client asks for, and it is in that state already. Same as adding an existing co-speaker (Co-speakers R5). |
| The talk is already in another room or slot (R5) | 409, nothing changes | "To move a talk, take it out first." |
| A main speaker or co-speaker of the talk is a main speaker or co-speaker of a talk in another room in the same slot (R6) | 409, nothing is saved | A conflict with the current programme. A person is one speaker ID. |
| Order of the checks | Path (400), body (400), talk exists (400), R3, R4, same talk in the same place (200), R5 room and slot taken, R5 talk elsewhere, R6 | The most basic error first. The double-click check comes before R5, so it is not refused as "already on the programme". |
| GET or DELETE on an empty room and slot | 404 | Nothing is there. The Entry in the request always has a `talkId`. |
| DELETE | 204 with no body. The room and slot are empty afterwards. | Same as unregistering from a workshop (workshop registration R9). |
| DELETE twice | 204, then 404 | The second request finds nothing, as in workshop registration R9. |
| Request body on GET or DELETE | Ignored | The request gives no body. |
| A talk on the programme changes afterwards: it is withdrawn, or gets or loses co-speakers (R7) | The entry stays. GET shows it, and the committee can take it out with DELETE. | The request says so. R6 is checked again only when a talk is put on the programme. |
| DELETE of a talk that is no longer `ACCEPTED` | 204 | R7 keeps the entry, so the committee must be able to remove it. |
| Concurrency (R8) | A PUT or DELETE is one transaction. It locks the slot for all three rooms with `SELECT … FOR UPDATE` on one fixed row for each slot, and reads the talk with `FOR SHARE`. Unique indexes on (room, slot) and on the talk back R5. | Advisory locks are not available on the platform. R6 compares the talks of all three rooms in the slot, so locking only one room and slot is not enough. The talk can be put in two different slots at the same time, and the unique index on the talk refuses the second one. |
| Who can change the programme | Anyone, no authentication | Same as the other endpoints today (tech debt B11). |
| Our Hurl examples | `examples/local/our-scheduling.hurl`, run only against a local build, never against the team URL. For the claim, the team-URL evidence is the harness checks plus `examples/run.sh` for the other files. | Each group empties the places it uses, and on the team URL that can delete the harness's own entries. `examples/run.sh` takes only the files directly in `examples/`. Coordinator, 2026-11-06. |
| An overview of the whole programme or of the empty places | Not built now | The request does not ask for it. The product owner wished for an overview of the gaps on 2026-11-06 at 13:09, but that wish is not part of this request. It is a later wish. Coordinator, 2026-11-06: do not ask the product owner. |

## Examples

Ada, Bob and Cy are registered speakers. "Talk of Ada" is an `ACCEPTED` `PRESENTATION` with Ada as
main speaker and no co-speakers, unless the row says otherwise. A talk is accepted after ratings
from two reviewers and the committee's decision (Acceptance R2, R3).

### R1: the fixed venue

| Room | Slot | Status of PUT | `start` | `end` |
|------|------|---------------|---------|-------|
| `R1` | `S1` | 200 | `09:00` | `10:00` |
| `R2` | `S2` | 200 | `10:15` | `11:15` |
| `R3` | `S3` | 200 | `11:30` | `12:30` |
| `R1` | `S4` | 200 | `13:30` | `15:30` |
| `R2` | `S5` | 200 | `15:45` | `16:45` |
| `R4` | `S1` | 400 | | |
| `R1` | `S6` | 400 | | |
| `r1` | `S1` | 400 | | |
| `R1` | `s1` | 400 | | |

GET and DELETE give 400 for the same four paths that are not in the list.

### R2: put, look up and take out

| Given | Request | Status | Response |
|-------|---------|--------|----------|
| `R1` `S1` empty | PUT talk A in `R1` `S1` | 200 | `{"room": "R1", "slot": "S1", "start": "09:00", "end": "10:00", "talkId": A}` |
| A in `R1` `S1` | GET `R1` `S1` | 200 | the same Entry |
| `R1` `S2` empty | GET `R1` `S2` | 404 | |
| A in `R1` `S1` | DELETE `R1` `S1`, then GET `R1` `S1` | 204, then 404 | |
| `R1` `S1` empty | DELETE `R1` `S1` | 404 | |
| A taken out of `R1` `S1` | PUT A in `R2` `S3` | 200 | Entry for `R2` `S3` |
| A in `R1` `S1` | PUT A in `R1` `S1` again | 200 | the same Entry |

### R2: the PUT body

| Body | Status |
|------|--------|
| no body | 400 |
| `{"talkId":` (not JSON) | 400 |
| `{}` | 400 |
| `{"talkId": null}` | 400 |
| `{"talkId": "<A>"}` (a string) | 400 |
| `{"talkId": <A>.0}` | 400 |
| `{"talkId": 999999999}` (no such talk) | 400 |
| `{"talkId": 0}` | 400 |

### R3: only an accepted talk

| Talk status | Status of PUT |
|-------------|---------------|
| `ACCEPTED` | 200 |
| `SUBMITTED`, `WAITLISTED`, `REJECTED` or `WITHDRAWN` | 409 |

### R4: a workshop needs the long slot

| Format | Slot | Status of PUT |
|--------|------|---------------|
| `WORKSHOP` | `S4` | 200 |
| `WORKSHOP` | `S1`, `S2`, `S3` or `S5` | 409 |
| `LIGHTNING_10`, `LIGHTNING_20` or `PRESENTATION` | `S4` | 200 |
| `LIGHTNING_10`, `LIGHTNING_20` or `PRESENTATION` | `S1` | 200 |

### R5: one talk per room and slot, and once on the programme

| Given | Request | Status | Afterwards |
|-------|---------|--------|------------|
| B in `R1` `S1` | PUT A in `R1` `S1` | 409 | GET `R1` `S1` shows B |
| A in `R1` `S1` | PUT A in `R2` `S1` | 409 | GET `R2` `S1` is 404 |
| A in `R1` `S1` | PUT A in `R1` `S2` | 409 | GET `R1` `S2` is 404 |
| A taken out of `R1` `S1` | PUT A in `R1` `S2` | 200 | GET `R1` `S1` is 404 |

### R6: nobody speaks in two rooms at the same time

| In `R1` `S2` | PUT in `R2` `S2` | Status |
|--------------|------------------|--------|
| a talk of Ada | another talk of Ada | 409 |
| a talk of Ada | a talk of Bob with co-speaker Ada | 409 |
| a talk of Bob with co-speaker Ada | a talk of Ada | 409 |
| a talk of Bob with co-speaker Ada | a talk of Cy with co-speaker Ada | 409 |
| a talk of Ada | a talk of Bob | 200 |
| a talk of Ada | another talk of Ada, but in `R2` `S3` | 200 |

### R7: later changes leave the programme as it is

| Given | Request | Status | Response |
|-------|---------|--------|----------|
| talk A in `R1` `S1`, then A is withdrawn | GET `R1` `S1` | 200 | Entry with A |
| the same | DELETE `R1` `S1` | 204 | |
| a talk of Ada in `R1` `S2` and a talk of Bob in `R2` `S2`, then Ada becomes co-speaker of Bob's talk | GET `R1` `S2` and GET `R2` `S2` | 200, 200 | both entries unchanged |

### R8: at the same time

| Given | Requests at the same time | Result |
|-------|---------------------------|--------|
| `R1` `S1` empty | 10 PUTs of 10 different accepted talks in `R1` `S1` | one 200, nine 409 |
| an empty programme | 10 PUTs of one talk in 10 different rooms and slots | one 200, nine 409; the talk is on the programme once |
| `S2` empty | PUT a talk of Ada in `R1` `S2` and another talk of Ada in `R2` `S2` | one 200, one 409 |

Hurl sends one request at a time, so the R8 rows are in the JUnit test only. The Hurl file for the other
rows is `examples/local/our-scheduling.hurl` (local build only, see "Our decisions").

## Effect on existing features

- Talks, ratings, acceptance and co-speakers: no change. A PUT reads the talk's status, format,
  main speaker and co-speakers, and changes none of them. A talk on the programme can still be
  withdrawn, and its co-speakers can still change (R7).
- Workshop registration: no change. Registration does not depend on the programme.
- `/admin/stats` and the CSV export: no change.

## Open questions for the product owner

None.
