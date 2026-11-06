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

## API

| Method | Path | Request body | Success status | Response body |
|--------|------|--------------|----------------|---------------|
| PUT | `/talks/{id}/co-speakers/{speakerId}` | none | 200 | Talk |
| DELETE | `/talks/{id}/co-speakers/{speakerId}` | none | 200 | Talk |

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
| The main speaker as co-speaker | 409 `the main speaker can't be a co-speaker` | R3. A conflict with the talk's state (its `speakerId`), not a malformed request. |
| Adding an existing co-speaker, also when the talk has two | 200, talk unchanged | R5. A double-click must not fail. The co-speaker keeps their place in the order. |
| Removing a registered speaker who isn't a co-speaker | 200, talk unchanged | DELETE is idempotent, like R5 for PUT: a double-click on remove must not fail. |
| Removing the main speaker | 409 `the main speaker can't be removed` | R8. A conflict with the talk's state. |
| Removing a co-speaker and adding them again | They go last | R4: the order they were added. |
| Withdrawn or rejected talk | 409 `a withdrawn or rejected talk can't be changed` | R9. A talk gets these statuses through the `acceptance` endpoints, `POST /talks/{id}/withdraw` and `PUT /talks/{id}/status`. The talk row lock makes a co-speaker change and a status change wait for each other. Checked after the 404s, before the other rules. |
| Order of checks | talk 404, speaker 404, R9, R3/R8, R5, R2 | The most basic error first. R5 before R2, so re-adding a co-speaker at the limit is still 200. |
| A co-speaker's own talk list | Unchanged: main-speaker talks only | R6. Applies to `GET /speakers/{id}/talks` and `GET /talks?speakerId=`. |
| Request body on PUT or DELETE | Ignored | The request says no body. |
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
| none | Main | 409 | [] |
| none | Nobody | 404 | [] |
| none | `abc` | 400 | [] |

### R8: removing a co-speaker

| Co-speakers before | DELETE | Status | `coSpeakerIds` after |
|--------------------|--------|--------|----------------------|
| [Ada, Bob] | Ada | 200 | [Bob] |
| [Ada] | Bob | 200 | [Ada] |
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
