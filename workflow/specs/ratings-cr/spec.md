# Ratings change request (`ratings-cr`)

Source: `request.md` in this folder (the change request from the harness, not edited). It changes
[Ratings](../ratings/spec.md) R4 (the average) and so R6 (the ranking). Everything else in Ratings
stays: a score is a whole number from 1 to 5 (Ratings R2), and a reviewer has one rating per talk
(Ratings R3).

## Rules

| ID | Rule |
|----|------|
| R1 | With fewer than 5 ratings, `averageRating` is the mean of every score, as before. |
| R2 | With 5 or more ratings, `averageRating` leaves out exactly one highest and exactly one lowest score, even when several reviewers gave the same top or bottom score. |
| R3 | `averageRating` is rounded to 2 decimals. A tie at the third decimal goes to the even neighbour (banker's rounding): `3.125` becomes `3.12`, `3.375` becomes `3.38`. |
| R4 | `ratingCount` counts every reviewer, also the two that R2 leaves out of the average. |
| R5 | `GET /talks?sort=rating` ranks by the new, rounded average. The rest of the ranking is as before (Ratings R6). |
| R6 | A talk nobody has rated has no average (`null`), as before. |

## API

No new endpoints. The changed field is `averageRating` on every Talk.

| Method | Path | Request body | Success status | Response body |
|--------|------|--------------|----------------|---------------|
| GET | `/talks/{id}`, `/talks`, `/talks?speakerId=`, `/talks?sort=rating`, `/speakers/{id}/talks` | | 200 | Talk(s) with the new `averageRating` |
| POST | `/talks`, `PUT /talks/{id}/status`, `POST /talks/{id}/withdraw`, the co-speaker PUT and DELETE | as before | as before | Talk with the new `averageRating` |

## Our decisions

| Case | Decision | Why |
|------|----------|-----|
| How the average is computed | Exactly, with `BigDecimal`: `(sum − max − min) / (count − 2)` for 5 or more ratings, `sum / count` otherwise, then `divide(…, 2, RoundingMode.HALF_EVEN)`. No `double` before the rounding. | R3 needs the exact mean. A `double` mean like `4.025` is stored as `4.02500000000000035…` and rounds the wrong way (examples in R3). Dividing two `BigDecimal` integers to scale 2 rounds the exact fraction. |
| Where the scores come from | The one grouped ratings query gets `MIN(score)` and `MAX(score)` next to `COUNT(*)` and `SUM(score)`. | Leaving out one highest and one lowest score is the same as subtracting `MAX` and `MIN` from the sum. No need to read every score. Still one query for a whole talk list. |
| Not in SQL | We do not use `AVG` or `ROUND` in Postgres. | `ROUND(numeric, 2)` rounds halves away from zero, not to even. |
| Where the code lives | On `main` now, before PR #8: `RatingDao.totalsFor` (the one ratings query for a talk list) also reads `MIN(score)` and `MAX(score)`, and `RatingTotals` computes the average from count, sum, min and max. `ConferenceService` takes the average from there. When PR #8 (`refactor/tech-debt-plan`) merges, it moves this into its `RatingSummary` and `TalkService`. | The coordinator decided on 2026-11-06 that this change ships before PR #8. `RatingTotals` already holds a talk's count and sum, so the average belongs next to them, and the list keeps its single query (no N+1). |
| JSON number of `averageRating` | A JSON number from a Java `Double`, as today: `3.12`, `3.1` (not `3.10`), `4.0` (not `4` or `4.00`), `null`. | The JSON grammar allows the text `3.10`, but every JSON parser reads it as the same number as `3.1`, so trailing zeros carry no information for a client or the harness. A `Double` keeps the format of every other talk answer and of the base examples (`4.5`, `5.0`) unchanged. The 2-decimal `BigDecimal` turns into the nearest `Double`, and Jackson writes its shortest form, which is the 2-decimal text. |
| Value the ranking compares | The rounded average (the same `Double` the answer shows). | R5. Two talks with the same rounded average are tied, even when their exact means differ; the tie goes on to the rating count (R5 examples). |
| Tie-breaks in the ranking | As today: higher rounded average first, then more ratings first, then oldest (lowest id) first. Unrated talks last, oldest first. `WITHDRAWN` talks after all the others (Acceptance R9). | Ratings R6 and Acceptance R9 are unchanged. |
| A reviewer who rates again | Counts once, with the latest score, for both the 5-rating threshold and the trim. | Ratings R3: one rating per reviewer. |
| Mean whose third decimal is not exactly 5 (e.g. `3.1251`, `3.333…`) | Ordinary rounding to the nearest 2-decimal value (`3.13`, `3.33`). | Only an exact tie goes to the even neighbour; that is what `HALF_EVEN` does. |
| CSV export `GET /admin/talks.csv`, column `rating` | Shows the talk's new `averageRating`, written as today (`String.valueOf` of the `Double`, comma for the point): `3,12`, `3,1`, `4,0`. | Ratings R4: a talk shows its average wherever it is shown. No harness spec covers the CSV, so we keep its format and change only the value. |
| Statistics `GET /admin/stats`, field `averageRating` | Unchanged formula: the mean of the talks' `averageRating` values, rounded to one decimal as it is today. Its input is now the new talk averages. | It is not a talk's `averageRating`, and the request says everything else stays. No harness spec covers stats. See the open question. |
| Acceptance's decision "Rounding of `averageRating`: half up, as before" (acceptance `spec.md`) | Replaced by R3 of this change request. | The change request comes later and names the rounding explicitly. |

## Examples

Each score comes from a different reviewer unless the row says otherwise. The average and the count
are read from `GET /talks/{id}`. A JSON value is written as the response has it.

### R1: fewer than 5 ratings, every score counts

| Scores | `averageRating` | `ratingCount` |
|--------|-----------------|---------------|
| 5 | 5.0 | 1 |
| 4, 5 | 4.5 | 2 |
| 1, 1, 2 | 1.33 | 3 |
| 1, 2, 2 | 1.67 | 3 |
| 4, 4, 4, 5 | 4.25 (was 4.3) | 4 |
| 1, 5, 5, 5 | 4.0 (no trim at 4) | 4 |
| reviewer 1: 1, then 5; reviewers 2, 3, 4: 5, 5, 1 | 4.0 (4 ratings: 5, 5, 5, 1; no trim) | 4 |

### R2: 5 or more ratings, exactly one highest and one lowest left out

| Scores | Kept | `averageRating` | `ratingCount` |
|--------|------|-----------------|---------------|
| 1, 2, 3, 4, 5 | 2, 3, 4 | 3.0 | 5 |
| 5, 5, 5, 1, 1 | 5, 5, 1 | 3.67 (plain mean 3.4) | 5 |
| 1, 5, 5, 5, 5 | 5, 5, 5 | 5.0 (4 ratings 1, 5, 5, 5 give 4.0) | 5 |
| 5, 5, 5, 5, 5 | 5, 5, 5 | 5.0 | 5 |
| 1, 1, 1, 1, 5 | 1, 1, 1 | 1.0 | 5 |
| 1, 2, 2, 3, 4, 5 | 2, 2, 3, 4 | 2.75 | 6 |
| reviewer 1: 5, then 1; reviewers 2–5: 1, 3, 4, 5 | 1, 3, 4 (latest scores 1, 1, 3, 4, 5) | 2.67 | 5 |

### R3: two decimals, ties to even, computed exactly

| Scores | Exact mean of the kept scores | `averageRating` | A `double` mean gives | `ratingCount` |
|--------|-------------------------------|-----------------|-----------------------|---------------|
| 5, 4, 3×7, 1 | 25 / 8 = 3.125 | 3.12 | | 10 |
| 5, 4×3, 3×5, 1 | 27 / 8 = 3.375 | 3.38 | | 10 |
| 5×2, 4×39, 1 | 161 / 40 = 4.025 | 4.02 | 4.03 | 42 |
| 5×20, 4×21, 1 | 179 / 40 = 4.475 | 4.48 | 4.47 | 42 |
| 5, 3×7, 2×33, 1 | 87 / 40 = 2.175 | 2.18 | 2.17 | 42 |
| 1, 1, 2 | 4 / 3 = 1.333… | 1.33 | | 3 |

`n×s` means `n` reviewers who each gave score `s`. The column "A `double` mean gives" is what
`new BigDecimal(sum / (double) count).setScale(2, HALF_EVEN)` answers: the wrong value. It is not in
the response; it shows why these rows are in the table.

### R4: `ratingCount` counts every reviewer

| Scores | `averageRating` | `ratingCount` |
|--------|-----------------|---------------|
| 5, 5, 5, 1, 1 | 3.67 | 5 |
| 5×2, 4×39, 1 | 4.02 | 42 |
| reviewer 1: 2, then 5 | 5.0 | 1 |

### R5: the ranking uses the rounded average

Talks are created in the order A, B, C, D (A oldest). `GET /talks?sort=rating`.

| A | B | C | D | Order |
|---|---|---|---|-------|
| 5, 4, 3×7, 1 (3.12, 10 ratings) | 5, 4×2, 3×15, 1 (53 / 17 = 3.1176…, shows 3.12, 19 ratings) | | | B, A (same rounded average, B has more ratings, although A's exact mean is higher) |
| 4, 5 (4.5, 2 ratings) | 4, 5 (4.5, 2 ratings) | | | A, B (same average and count: oldest first) |
| 5, 5, 5, 1, 1 (3.67) | 1, 5, 5, 5 (4.0) | 5, 5, 5, 5, 1 (5.0) | | C, B, A |
| none | 4, 4, 4, 5 (4.25) | 4, 5 (4.5) | none | C, B, A, D (unrated last, oldest first) |
| 4, 5, `WITHDRAWN` | 3 | none | | B, C, A (withdrawn after all others, Acceptance R9) |

### R6: no ratings, no average

| Scores | `averageRating` | `ratingCount` |
|--------|-----------------|---------------|
| none | `null` | 0 |

## Effect on existing features

- Ratings R4: the average is no longer one decimal, half up. `RatingsTest` row "Average with a
  second half" (`4, 4, 4, 5` → `4.3`) becomes `4.25`; the display name changes to R1–R3 of this
  spec. The row "One reviewer rates again" (`5.0`) and "Two reviewers rate once" (`3.5`) stay.
- Ratings R6: the ranking compares the 2-decimal average. `TalkListsTest` and Acceptance R9's
  ranking rows (`5`, `3`, `4` → C, B, A) keep their order, as one-score averages do not change.
- Ratings R2, R3, R7, R8 and every other field of a Talk: no change. `RatingsTest`,
  `RatingConcurrencyTest` and `RatedAtDisplayTest` show it.
- Acceptance: the decision "Rounding of `averageRating`: half up" is replaced by R3. Acceptance R3
  (two reviewers before accepting) counts reviewers, not the average: no change.
- Admin CSV and stats: see "Our decisions".

## Open questions for the product owner

- [ ] The committee statistics (`GET /admin/stats`) show one conference-wide `averageRating`: the
  mean of the talks' averages, with one decimal. Should it follow the new rule (the mean of the
  trimmed talk averages, as we plan), or keep using every score of every talk, and should it also
  show two decimals? It is not in any spec on the harness, so the question does not block us.
