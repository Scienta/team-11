# Talk submissions (`talks`)

Source: `request.md` in this folder (the base spec from the harness, not edited).

## Rules

The organisers' `examples/talks.hurl` refers to R1, R2, R4, R6, R8, R9 and R10. The numbers below
follow the order of the request, and they match every rule that the Hurl file names. R3, R5 and R7
are not named there, so their numbers are our reading of the same order.

| ID | Rule |
|----|------|
| R1 | A speaker submits a talk with a title, a format and a level. |
| R2 | The speaker can look the talk up again later. |
| R3 | Every talk belongs to a speaker who is already registered, and has a real title. |
| R4 | The format decides the duration: `LIGHTNING_10` 10, `LIGHTNING_20` 20, `PRESENTATION` 45, `WORKSHOP` 120 minutes. Nobody else picks the length. |
| R5 | The levels are `BEGINNER`, `INTERMEDIATE` and `ADVANCED`. Formats and levels are written exactly like this, in capitals, and nothing else is a format or a level. |
| R6 | A new talk is `SUBMITTED`. |
| R7 | A speaker submits at most 3 talks, also when several submissions arrive at the same time. |
| R8 | A speaker can list all of their own talks, oldest first. |
| R9 | `submittedAt` is the submission time in UTC, as `yyyy-MM-dd'T'HH:mm:ss.SSS+00:00`: always with milliseconds, never `Z`. |
| R10 | `submittedOn` is the UTC date of `submittedAt`, as `yyyy-MM-dd`. |

## API

| Method | Path | Request body | Success status | Response body |
|--------|------|--------------|----------------|---------------|
| POST | `/talks` | `{title, format, level, speakerId}` | 201 | Talk |
| GET | `/talks/{id}` | | 200 | Talk |
| GET | `/speakers/{id}/talks` | | 200 | list of Talk, oldest first |

A Talk is `{id, title, format, level, speakerId, durationMinutes, status, submittedAt, submittedOn,
averageRating, ratingCount}`. The ratings feature owns `averageRating` and `ratingCount`.

## Our decisions

The feature request does not give these. They describe what the service does today.

| Case | Decision | Why |
|------|----------|-----|
| Title empty, only spaces or missing | 400 | R3: not a real title. |
| Title longer than 200 characters | 400 | R3: a real title is not a text of any length. A character is one Unicode code point, as for the 500-character bio (Speakers R4). Security review S12, decided by The Hand 2026-11-06: a long title came back in every list and in the CSV. The harness sends titles of at most 51 characters (request log, 2026-11-06 13:14 to 13:28 UTC). |
| Format or level not in the list, in lower case, or missing | 400 | R5 |
| `speakerId` missing | 400 | A missing field. |
| `speakerId` of a speaker who is not registered | 400 | R3. The ID is a field in the body, not a resource in the path, so it is a bad request, not a 404. |
| A 4th talk from the same speaker | 409 | R7: a conflict with the speaker's current talks. |
| `durationMinutes` or another unknown field in the request | Ignored | R4: the client cannot choose the length. |
| Unknown talk ID in `GET /talks/{id}` | 404 | Same as the base features. |
| Talk or speaker ID that is not a number | 400 | Same as the speakers feature. |
| Unknown speaker in `GET /speakers/{id}/talks` | 404 | The speaker is the resource in the path. |

## Examples

The tables are `@TableTest`s in `src/test/java/no/scienta/cfp/TalksTest.java`. The report for the
product owners is in `docs/specs/talks/`.

### R3: a real title

| Title | Status |
|-------|--------|
| `Legacy is a feeling` | 201 |
| empty | 400 |
| only spaces | 400 |
| missing | 400 |
| `x` × 200 | 201 |
| `x` × 201 | 400 |
| `😀` × 200 | 201 |
| `😀` × 201 | 400 |

### R4: the format sets the duration

| Format | `durationMinutes` |
|--------|-------------------|
| `LIGHTNING_10` | 10 |
| `LIGHTNING_20` | 20 |
| `PRESENTATION` | 45 |
| `WORKSHOP` | 120 |

### R5: only the listed formats and levels, in capitals

| Format | Level | Status |
|--------|-------|--------|
| any of the four formats | `BEGINNER` | 201 |
| `KEYNOTE` | `BEGINNER` | 400 |
| `presentation` | `BEGINNER` | 400 |
| missing | `BEGINNER` | 400 |
| `PRESENTATION` | any of the three levels | 201 |
| `PRESENTATION` | `EXPERT` | 400 |
| `PRESENTATION` | `beginner` | 400 |
| `PRESENTATION` | missing | 400 |

### R7: at most 3 talks, also at the same time

| Given | Request | Status |
|-------|---------|--------|
| a speaker with no talks | 10 submissions at the same time | three 201, seven 409; the speaker lists 3 talks |

R1, R2, R6, R8, R9 and R10 have no table of their own yet. The organisers' `examples/talks.hurl`
checks them on one workshop and one lightning talk.

## Effect on existing features

- Ratings: every Talk carries `averageRating` and `ratingCount` (ratings R4).
- Acceptance (drip feature): `status` can change after R6. The product owner said (2026-11-06
  13:07) that a withdrawn talk does not count towards the limit of R7, and a rejected talk does.

## Open questions for the product owner

None.
