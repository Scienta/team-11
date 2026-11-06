# Ratings (`ratings`)

Source: `request.md` in this folder (the base spec from the harness, not edited).

## Rules

| ID | Rule |
|----|------|
| R1 | A reviewer is the number in the URL, a positive whole number. Reviewers are not registered. |
| R2 | A score is a whole number from 1 to 5. Nothing else is a score. |
| R3 | A reviewer has at most one rating per talk. A new rating replaces the old score. |
| R4 | Each talk shows `ratingCount` and `averageRating` wherever it is shown. The average has one decimal, halves rounded up. No ratings: no average (`null`). |
| R5 | The committee lists all talks, oldest first, or the talks of one speaker. |
| R6 | The ranking: best shown average first, then more ratings first, then oldest first. Unrated talks come last. |
| R7 | Concurrent ratings and double-clicks never lose a rating and never count one twice. |
| R8 | `ratedAtDisplay` is the time of the latest rating, in UTC, as `M/d/yy, h:mm AM`. |

## API

| Method | Path | Request body | Success status | Response body |
|--------|------|--------------|----------------|---------------|
| PUT | `/talks/{id}/ratings/{reviewerId}` | `{score}` | 200 | `{talkId, reviewerId, score, ratedAtDisplay}` |
| GET | `/talks?speakerId=&sort=rating` | | 200 | list of Talk |

## Our decisions

| Case | Decision | Why |
|------|----------|-----|
| Reviewer 0 or negative | 400 | R1 |
| Reviewer above 2147483647 | Accepted, up to 9223372036854775807 | R1 says any positive whole number. The column is `BIGINT`. |
| Reviewer above 9223372036854775807, or not a number | 400 | It does not fit a 64-bit number. |
| Score `4.0`, `"4"`, `true`, missing | 400 | R2: only a JSON integer is a score. |
| Score 0 or 6 | 400 | R2 |
| Unknown talk | 404 | |
| `sort` other than `rating` | 400 | |
| Unknown `speakerId` in the list | 200 with an empty list | It is a filter, not a lookup. |

## Examples

### R1: the reviewer is a positive whole number

| Reviewer | Status |
|----------|--------|
| 1 | 200 |
| 2147483647 | 200 |
| 2147483648 | 200 |
| 9223372036854775807 | 200 |
| 9223372036854775808 | 400 |
| 0 | 400 |
| -1 | 400 |

### R4: the average

| Scores | `averageRating` | `ratingCount` |
|--------|-----------------|---------------|
| none | `null` | 0 |
| 4, 5 | 4.5 | 2 |
| 4, 4, 4, 5 | 4.3 | 4 |

## Effect on existing features

The talk responses carry `averageRating` and `ratingCount`. The talks feature owns the other fields.

## Open questions for the product owner

- [ ] Is a reviewer number above 2147483647 realistic? We accept it, so the question does not block us.
