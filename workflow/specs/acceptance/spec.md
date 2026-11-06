# Acceptance state machine (`acceptance`)

Source: `request.md` in this folder (the feature request from the harness, not edited).

## Rules

R1–R6 come from the feature request. R7–R9 come from the product owner's answers in Slack #team-11
on 2026-11-06.

| ID | Rule |
|----|------|
| R1 | The committee decides on a talk: `ACCEPTED`, `REJECTED` or `WAITLISTED`. |
| R2 | A `SUBMITTED` talk can get any decision. A `WAITLISTED` talk can still become `ACCEPTED` or `REJECTED`. An `ACCEPTED` or `REJECTED` talk gets no other decision. |
| R3 | The committee accepts a talk only when at least two different reviewers have rated it. A reviewer who rated twice is one reviewer. Rejecting and waitlisting need no ratings. |
| R4 | The main speaker of a talk can withdraw it, also when it is `ACCEPTED`, but not when it is `REJECTED` or `WITHDRAWN`. Nobody else can withdraw it. A `WITHDRAWN` talk never changes again. |
| R5 | The talk's current `status` shows wherever the talk is shown. |
| R6 | A decision or withdrawal that is not allowed changes nothing. Two committee members who decide on the same talk at the same time never leave it in a state that R2–R4 forbid. |
| R7 | A `WITHDRAWN` or `REJECTED` talk does not count towards the limit of 3 talks for each speaker. (Changed by the change request [submission-cap-cr](../submission-cap-cr/spec.md): before, a `REJECTED` talk counted.) |
| R8 | Nobody can rate a talk that is `ACCEPTED`, `REJECTED` or `WITHDRAWN`. |
| R9 | `GET /talks` and `GET /talks?sort=rating` show `WITHDRAWN` talks after all the other talks. A client can leave out the `WITHDRAWN` talks. |

## API

| Method | Path | Request body | Success status | Response body |
|--------|------|--------------|----------------|---------------|
| PUT | `/talks/{id}/status` | `{"status": "ACCEPTED" \| "REJECTED" \| "WAITLISTED"}` | 200 | Talk |
| POST | `/talks/{id}/withdraw` | `{"speakerId": <integer>}` | 200 | Talk |
| GET | `/talks?includeWithdrawn=false` | | 200 | list of Talk, without `WITHDRAWN` talks |

`Talk.status` is one of `SUBMITTED`, `ACCEPTED`, `REJECTED`, `WAITLISTED`, `WITHDRAWN`.

## Our decisions

| Case | Decision | Why |
|------|----------|-----|
| Unknown talk in the path | 404 | Same as the base features |
| Missing body, malformed JSON | 400 | Same as the base features |
| `status` missing, not a string, `SUBMITTED`, `WITHDRAWN`, lower case or unknown | 400 | Only the three decisions in R1 are decisions. A withdrawal has its own endpoint. |
| `speakerId` missing or not a JSON integer | 400 | Same strictness as the score in Ratings R2 |
| Decision that R2 forbids, for example `REJECTED` → `ACCEPTED` | 409, the talk does not change (R6) | A conflict with the current state, as for the talk limit |
| The same decision again: `WAITLISTED` → `WAITLISTED`, `ACCEPTED` → `ACCEPTED` | 409, the talk does not change | R2 lists only the moves to another status. The request says that a move that is not allowed "changes nothing", and a repeat changes nothing too, so 409 tells the second committee member that someone else already decided. Confirmed by the coordinator, 2026-11-06. |
| `ACCEPTED` with fewer than two different reviewers | 409, the talk does not change | R3 is a rule about the current state of the talk |
| Withdrawal by a `speakerId` that is not the talk's main speaker, also an unknown speaker | 403, the talk does not change | R4: nobody else can withdraw. Who asks is checked before the status, so a stranger learns nothing about the status. |
| Withdrawal of a `REJECTED` or `WITHDRAWN` talk by its main speaker | 409, the talk does not change | R4 |
| Rating a talk that R8 closes | 409, the rating is not saved | R8 is a conflict with the current state |
| Order of `WITHDRAWN` talks in a list (R9) | Last, in the same order as the list uses for the other talks (oldest first, or the ranking of Ratings R6) | The PO said "apart from the others (for example at the end)". The talks that are not withdrawn keep exactly the order that Ratings R5 and R6 give. With no withdrawn talk, the output is the same as before. |
| How a client leaves out `WITHDRAWN` talks (R9) | `includeWithdrawn=false`. The default is `true`, also for an empty value (`includeWithdrawn=`). Any other value gives 400, also `yes`, `1` and `TRUE`. | The PO asked for a filter, not for a name. A default of `true` keeps the base list as it was. |
| `GET /talks?speakerId=` (R9) | Withdrawn talks last, as in `GET /talks` | It is the same list with a filter (PO question 5b, "one speaker's talks"). |
| `GET /speakers/{id}/talks` | No change: withdrawn talks stay in place, oldest first | The PO named only `GET /talks`. Coordinator, 2026-11-06. |
| Concurrency (R6) | Each change is one conditional `UPDATE … WHERE status IN (<statuses that allow it>)`. A rating locks the talk row (`FOR SHARE`) while it checks R8 and saves. | Advisory locks are not available on the platform. A decision cannot slip in between the check and the write. |
| Rounding of `averageRating` | Half up, as before | PO, 2026-11-06. Do not ask again. |

## Examples

Each table is a `@TableTest` in `AcceptanceTest` and a request in `examples/our-acceptance.hurl`. "Status
before" is the status the talk gets through the API before the request. Every talk has ratings from
two reviewers before it gets its status, except in the R3 table.

### R1, R2, R6: the committee's decision

| Status before | Decision | Response | Status after |
|---------------|----------|----------|--------------|
| `SUBMITTED` or `WAITLISTED` | `ACCEPTED` | 200 | `ACCEPTED` |
| `SUBMITTED` or `WAITLISTED` | `REJECTED` | 200 | `REJECTED` |
| `SUBMITTED` | `WAITLISTED` | 200 | `WAITLISTED` |
| `WAITLISTED` | `WAITLISTED` | 409 | `WAITLISTED` |
| `ACCEPTED` | any decision | 409 | `ACCEPTED` |
| `REJECTED` | any decision | 409 | `REJECTED` |
| `WITHDRAWN` | any decision | 409 | `WITHDRAWN` |

### R3: two different reviewers before acceptance

| Ratings from reviewers | Decision | Response | Status after |
|------------------------|----------|----------|--------------|
| none | `ACCEPTED` | 409 | `SUBMITTED` |
| 1 | `ACCEPTED` | 409 | `SUBMITTED` |
| 1, 1 (reviewer 1 rates twice) | `ACCEPTED` | 409 | `SUBMITTED` |
| 1, 2 | `ACCEPTED` | 200 | `ACCEPTED` |
| none | `REJECTED` or `WAITLISTED` | 200 | the decision |

### R1: what a decision is

| Body | Response |
|------|----------|
| `{"status": "WAITLISTED"}` | 200 |
| `{"status": "SUBMITTED"}`, `{"status": "WITHDRAWN"}` | 400 |
| `{"status": "accepted"}`, `{"status": "MAYBE"}`, `{"status": ""}` | 400 |
| `{}` | 400 |

### R4: who can withdraw

| Speaker asking | Response | Status after |
|----------------|----------|--------------|
| the main speaker | 200 | `WITHDRAWN` |
| another registered speaker, an unknown speaker | 403 | `SUBMITTED` |
| no `speakerId` | 400 | `SUBMITTED` |

### R4, R6: which talks the main speaker can withdraw

| Status before | Response | Status after |
|---------------|----------|--------------|
| `SUBMITTED`, `WAITLISTED`, `ACCEPTED` | 200 | `WITHDRAWN` |
| `REJECTED` | 409 | `REJECTED` |
| `WITHDRAWN` | 409 | `WITHDRAWN` |

### R5: the status wherever the talk is shown

One speaker has one talk with the status in the table. Each place shows that status for the talk.

| Status | Place | Status shown |
|--------|-------|--------------|
| `SUBMITTED` | `GET /talks/{id}`, `GET /talks`, `GET /talks?speakerId=`, `GET /talks?sort=rating`, `GET /speakers/{id}/talks` | `SUBMITTED` |
| `ACCEPTED` | the same five places | `ACCEPTED` |
| `REJECTED` | the same five places | `REJECTED` |
| `WAITLISTED` | the same five places | `WAITLISTED` |
| `WITHDRAWN` | the same five places | `WITHDRAWN` |

### Unknown talk

| Request | Response |
|---------|----------|
| `PUT /talks/999999/status`, `POST /talks/999999/withdraw` | 404 |

### R6: two committee members at the same time

| Status before | Two requests at once | Responses | Status after |
|---------------|----------------------|-----------|--------------|
| `SUBMITTED` | `ACCEPTED` and `REJECTED` | one 200, one 409 | the decision that got 200 |
| `SUBMITTED` | `REJECTED` and a withdrawal | one 200, one 409 | the status that got 200 |

### R7: a withdrawn or rejected talk does not count towards the limit

The speaker has 3 talks, and the third has the status in the table. Then the speaker submits a new talk.
The `REJECTED` row changed with the change request submission-cap-cr ("change request overrides").

| Status of the third talk | Max talks | Response |
|--------------------------|-----------|----------|
| `SUBMITTED`, `ACCEPTED`, `WAITLISTED` | 3 | 409 |
| `WITHDRAWN`, `REJECTED` | 3 | 201 |

### R8: ratings only for open talks

| Status | Response | `ratingCount` after |
|--------|----------|---------------------|
| `SUBMITTED`, `WAITLISTED` | 200 | 3 |
| `ACCEPTED`, `REJECTED`, `WITHDRAWN` | 409 | 2 |

### R9: withdrawn talks in the lists

Talks A, B and C are submitted in this order. Each score comes from a different reviewer.

| Talk A | Talk B | Talk C | Withdrawn | List | Titles |
|--------|--------|--------|-----------|------|--------|
| 5 | 3 | 4 | A | `/talks`, `/talks?includeWithdrawn=true` | B, C, A |
| 5 | 3 | 4 | A | `/talks?speakerId=<the speaker>` | B, C, A |
| 5 | 3 | 4 | A | `/talks?sort=rating` | C, B, A |
| 5 | none | 4 | A | `/talks?sort=rating` | C, B, A |
| 5 | 3 | 4 | A, B | `/talks?sort=rating` | C, A, B |
| 5 | 3 | 4 | A | `/talks?includeWithdrawn=false` | B, C |
| 5 | 3 | 4 | A | `/talks?sort=rating&includeWithdrawn=false` | C, B |

### R9: the value of `includeWithdrawn`

| Value | Response |
|-------|----------|
| `true`, `false`, empty | 200 |
| `yes`, `1`, `TRUE` | 400 |

## Effect on existing features

- Talks: `status` takes all five values. `POST /talks` no longer counts `WITHDRAWN` talks (R7).
  `TalksTest` shows that the limit of 3 still holds.
- Ratings: `PUT /talks/{id}/ratings/{reviewerId}` answers 409 for a closed talk (R8). `RatingsTest`
  shows that rating an open talk still works.
- Ratings R5 and R6: the order of the talks that are not withdrawn does not change. `TalkListsTest`
  runs unchanged.
- Statistics: `byStatus` counts the new statuses. No other change.

## Open questions for the product owner

None. The product owner answered our questions in Slack #team-11 on 2026-11-06 (R7–R9 and the
decisions marked "PO").
