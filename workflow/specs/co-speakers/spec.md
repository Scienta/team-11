# Co-speakers (`co-speakers`)

Source: `request.md` in this folder (the feature request from the harness, not edited).

## Rules

| ID | Rule |
|----|------|
| R1 | A talk keeps its one main speaker (`speakerId`). Co-speakers are other registered speakers, added to the talk afterwards. |
| R2 | A talk has at most two co-speakers. |
| R3 | The main speaker can't be a co-speaker of their own talk. |
| R4 | Every talk shows `coSpeakerIds`, in the order they were added, `[]` when none, wherever a talk is shown: `POST /talks`, `GET /talks/{id}`, `GET /talks` (with and without `speakerId` and `sort=rating`), `GET /speakers/{id}/talks`, and the answers of `PUT /talks/{id}/status` and `POST /talks/{id}/withdraw` (acceptance). |
| R5 | Adding someone who is already a co-speaker changes nothing. |
| R6 | A speaker's own talk list shows only the talks where they are the main speaker. |
| R7 | The limit of 3 talks counts only the talks where the speaker is the main speaker. A speaker who has reached it can still co-speak. |
| R8 | Co-speakers can be removed again. The main speaker can't be removed. (Product owner, see below.) |
| R9 | A talk that is withdrawn or rejected can't be changed: no co-speakers added or removed. (Product owner, see below.) |
| R10 | Simultaneous adds and double-clicks never give a talk more than two co-speakers, and never the same co-speaker twice. |
| R11 | A co-speaker link belongs to its talk and its speaker: when either is deleted, the link goes too. (Ours, not in the request; see below.) |
| R12 | Only the main speaker may add or remove co-speakers on their talk. Checked only when the request says who acts, with the optional query parameter `actingSpeakerId`. (Product owner, see below.) |

## API

| Method | Path | Request body | Success status | Response body |
|--------|------|--------------|----------------|---------------|
| PUT | `/talks/{id}/co-speakers/{speakerId}[?actingSpeakerId=]` | none | 200 | Talk |
| DELETE | `/talks/{id}/co-speakers/{speakerId}[?actingSpeakerId=]` | none | 200 | Talk |

Talk is the Talk of the talks and ratings specs with one more field, `coSpeakerIds` (array of speaker
ids), placed last.

## Our decisions

| Case | Decision | Why |
|------|----------|-----|
| Unknown talk (PUT or DELETE) | 404 `talk not found` | An unknown ID in the path, same as `GET /talks/{id}`. Checked before the speaker. |
| Unknown speaker (PUT or DELETE) | 404 `speaker not found` | An unknown ID in the path, same as `GET /speakers/{id}`. Not 400 as on `POST /talks`, where the id is in the body. |
| Talk or speaker id that is not a number, or beyond a 64-bit number | 400 | Same as the base features' path ids. |
| Talk or speaker id 0 or negative | 404 | No talk or speaker has that id. |
| A third co-speaker | 409 `a talk has at most 2 co-speakers` | R2. A conflict with the talk's current state; removing one makes room. |
| The main speaker as co-speaker (PUT) | 400 `the main speaker can't be a co-speaker` | R3. The rules never allow this request, whatever the talk's state; no change to the talk makes it possible. Changed from 409 at tick 171: the harness check `co-speakers/check-2` rejected our 409. A 409 stays for a conflict with the current state that can go away (R2, R9). |
| Adding an existing co-speaker, also when the talk has two | 200, talk unchanged | R5. A double-click must not fail. The co-speaker keeps their place in the order. |
| Removing a registered speaker who isn't a co-speaker, or no longer is one | 404 `this speaker is not a co-speaker of the talk`, talk unchanged | The resource in the path, the speaker's place among the talk's co-speakers, does not exist. Changed from 200 at tick 171: the harness check `co-speakers/check-1` rejected our 200 ("DELETE is idempotent"). A second click on remove gets 404. |
| Removing the main speaker | 409 `the main speaker can't be removed`, unchanged at tick 171 | R8. Coordinator's decision at tick 171 (Fiffi): no harness check has rejected it. The alternative would be 404, because the main speaker is never a co-speaker. |
| Removing a co-speaker and adding them again | They go last | R4: the order they were added. |
| Withdrawn or rejected talk | 409 `a withdrawn or rejected talk can't be changed` | R9. A talk gets these statuses through the `acceptance` endpoints, `POST /talks/{id}/withdraw` and `PUT /talks/{id}/status`. The talk row lock makes a co-speaker change and a status change wait for each other. Checked after the 404s, before the other rules. |
| Order of checks | `actingSpeakerId` 400, talk 404, R12 403, speaker 404, R9, R3 (PUT, 400) or R8 (DELETE, 409), then for PUT R5, R2 and for DELETE "not a co-speaker" 404 | The most basic error first. R5 before R2, so re-adding a co-speaker at the limit is still 200. R12 is placed as acceptance places the withdrawal's 403: after the talk 404 and before the status (R9), so someone who may not act learns nothing about the talk's status or the speaker in the path. |
| A co-speaker's own talk list | Unchanged: main-speaker talks only | R6. Applies to `GET /speakers/{id}/talks` and `GET /talks?speakerId=`. |
| Request body on PUT or DELETE | Ignored | The request says no body. |
| Who acts (R12) | Optional query parameter `actingSpeakerId` on PUT and DELETE. Absent, or an empty value (`actingSpeakerId=`): no check, as before R12. | Product owner (Stein, 2026-11-06 14:21): yes, only the main speaker. The request defines no body for PUT, and the harness sends no actor, so a required actor would break the harness checks. An empty value counts as absent, as `includeWithdrawn=` does in acceptance. |
| `actingSpeakerId` that is not the talk's main speaker, also a co-speaker, an unknown speaker or 0 or below | 403 `only the main speaker can change the co-speakers`, the talk does not change | R12. Same as acceptance's withdrawal by someone else (403 `only the main speaker can withdraw the talk`), with Boot's usual error body. |
| `actingSpeakerId` that is not a whole number, or beyond a 64-bit number | 400 | Same as the base features' bad parameters and path ids. |
| A talk or speaker row is deleted (R11) | Its co-speaker links are deleted with it: foreign keys `talk_id REFERENCES talk(id) ON DELETE CASCADE` and `speaker_id REFERENCES speaker(id) ON DELETE CASCADE` | No API deletes talks or speakers, but a row deleted by hand left a link behind: a deleted speaker still showed in `coSpeakerIds` (R4) and took one of the two places (R2), and a link to a deleted speaker broke R1 (co-speakers are registered speakers). Cascade rather than restrict, because a link has no meaning without its talk and speaker, and restrict would make deleting a speaker fail for a reason nobody can see in the API. |
| Adding the R11 keys to the production table | `schema.sql` adds each key only when it is missing, `NOT VALID` first, then deletes the links to a talk or speaker that is gone, then validates. Any error is a Postgres warning, not a failed start-up; the next start-up tries again. | The table already exists in production with data, so `CREATE TABLE IF NOT EXISTS` with `REFERENCES` would add nothing, and a plain `ADD CONSTRAINT` fails on one orphan row, which stops start-up and the deploy. `NOT VALID` makes no new orphan possible before the old ones are deleted. Orphan links are deleted, not kept: they point at a talk or speaker that no longer exists. Proven by `CoSpeakerForeignKeysTest`. |

## Examples

Main is the talk's main speaker. Ada, Bob and Cy are other registered speakers. Nobody is an
unregistered speaker id.

### R1, R2, R3, R5: adding a co-speaker

| Co-speakers before | PUT | Status | `coSpeakerIds` after |
|--------------------|-----|--------|----------------------|
| none | Ada | 200 | [Ada] |
| [Ada] | Bob | 200 | [Ada, Bob] |
| [Ada, Bob] | Cy | 409 | [Ada, Bob] |
| [Ada] | Ada | 200 | [Ada] |
| [Ada, Bob] | Bob | 200 | [Ada, Bob] |
| none | Main | 400 | [] |
| none | Nobody | 404 | [] |
| none | `abc` | 400 | [] |

### R8: removing a co-speaker

| Co-speakers before | DELETE | Status | `coSpeakerIds` after |
|--------------------|--------|--------|----------------------|
| [Ada, Bob] | Ada | 200 | [Bob] |
| [Ada] | Bob | 404 | [Ada] |
| none, Ada removed before | Ada | 404 | [] |
| [Ada] | Main | 409 | [Ada] |
| [Ada] | Nobody | 404 | [Ada] |
| [Ada] | `abc` | 400 | [Ada] |

### Unknown talk

| Request | Status |
|---------|--------|
| PUT or DELETE on talk 2147483647 | 404 |
| PUT or DELETE on talk `abc` | 400 |

### R4: shown wherever a talk is shown, in the order added

| Given | Request | `coSpeakerIds` |
|-------|---------|----------------|
| a new talk | `POST /talks` | [] |
| Bob, then Ada added | `GET /talks/{id}`, `GET /talks`, `GET /talks?speakerId=`, `GET /talks?sort=rating`, `GET /talks?speakerId=&sort=rating`, `GET /speakers/{id}/talks`, `GET /talks?includeWithdrawn=false` | [Bob, Ada], last field of the talk |
| Ada added | `PUT /talks/{id}/status` `{"status":"WAITLISTED"}`, `POST /talks/{id}/withdraw` | [Ada], last field of the talk |
| [Ada, Bob], Ada removed and added again | `GET /talks/{id}` | [Bob, Ada] |

### R6: a co-speaker's own talk list

| Given | Request | Talks |
|-------|---------|-------|
| Ada co-speaks on Main's talk | `GET /speakers/{Ada}/talks` | [] |
| Ada co-speaks on Main's talk | `GET /talks?speakerId={Ada}` | [] |

### R7: the limit of 3 counts main-speaker talks only

| Given | Request | Status |
|-------|---------|--------|
| Ada has submitted 3 talks | PUT Ada as co-speaker on Main's talk | 200 |
| Ada co-speaks on Main's talk | Ada submits 3 talks | 201 each |

### R9: a withdrawn or rejected talk can't be changed

| Talk status | Request | Status |
|-------------|---------|--------|
| `WITHDRAWN` or `REJECTED` | PUT or DELETE Ada | 409 |
| `SUBMITTED`, `WAITLISTED` or `ACCEPTED` | PUT or DELETE Ada | 200 |

### R10: concurrency

| Given | Request | Result |
|-------|---------|--------|
| a talk with no co-speakers | 10 PUTs of 10 different speakers at the same time | two 200, eight 409, two co-speakers |
| a talk with no co-speakers | 10 PUTs of Ada at the same time | ten 200, `coSpeakerIds` [Ada] |

### R11: a deleted talk or speaker takes its links with it

| Given | Then |
|-------|------|
| [Ada, Bob] on Main's talk, Bob's speaker row deleted | `coSpeakerIds` [Ada]; PUT Cy gives 200, [Ada, Cy] |
| [Ada] on Main's talk, the talk row deleted | no link left for the talk |
| an existing table without keys, with links to talk 99 and speaker 99 that don't exist | start-up succeeds, deletes those two links, keeps the others, and both keys are validated and cascading |
| the schema run twice more | nothing changes |
| a key that can't be added (no primary key on `talk`) | the schema still runs to the end, without the keys |

### R12: only the main speaker, when the request says who acts

The talk has Ada as co-speaker. PUT adds Bob, DELETE removes Ada.

| `actingSpeakerId` | Status | `coSpeakerIds` after |
|-------------------|--------|----------------------|
| none, or empty | 200 | PUT [Ada, Bob], DELETE [] |
| Main | 200 | PUT [Ada, Bob], DELETE [] |
| Ada, Cy, Nobody, 0 or -1 | 403 | [Ada] |
| `abc`, `1.5`, `99999999999999999999` | 400 | [Ada] |

| Given | Request, `actingSpeakerId` Cy | Status |
|-------|-------------------------------|--------|
| unknown talk | PUT Bob or DELETE Ada | 404 |
| talk `SUBMITTED` | PUT or DELETE Nobody | 403 |
| talk `SUBMITTED` | PUT or DELETE Main | 403 |
| talk `WITHDRAWN` or `REJECTED` | PUT Bob or DELETE Ada | 403 (with Main acting: 409) |

## Effect on existing features

- Talks and ratings: every talk response gets `coSpeakerIds` as its last field. The other fields,
  their formats and their order are unchanged (`TalksTest`, `RatingsTest`, `examples/talks.hurl`,
  `examples/ratings.hurl`).
- Talks: the limit of 3 and the speaker's talk list are unchanged (`TalksTest.concurrentSubmissionsNeverExceedThreeTalks`,
  `CoSpeakersTest` R6 and R7).

## Open questions for the product owner

- [x] Can co-speakers be removed? Yes. The main speaker can't be removed. (Stein, #team-11, 2026-11-06 13:13)
- [x] Co-speakers on withdrawn, rejected or accepted talks? They can be added to an accepted talk, not
  to a rejected or withdrawn one. A withdrawn or rejected talk can't be changed at all, so co-speakers
  are added or removed only while it is `SUBMITTED`, `WAITLISTED` or `ACCEPTED`. (Stein, #team-11,
  2026-11-06 13:11 and 13:14)
- [x] Can a co-speaker withdraw a talk? No, only the main speaker. (Stein, #team-11, 2026-11-06 13:11)
  That is `acceptance`'s rule (`POST /talks/{id}/withdraw`).
- [x] Who may add or remove co-speakers? Only the main speaker on their talk. (Stein, 2026-11-06 14:21) That is R12.
