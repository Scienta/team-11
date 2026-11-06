# Claim evidence: ratings-cr

The rows come from [`../claim-checklist.md`](../claim-checklist.md). The product owner reads a claim
request in three sections: security, architecture and functionality.

Claim: **claimed at harness tick 195** by Nils Christian. Carl Kristian's team prepared the claim request
(PR #11). Deployed at the claim: `6227fcb` (`GET /health`; [deploy run 42235128898](https://github.com/Scienta/team-11/actions/runs/42235128898):
jobs `deploy` and `tests` success, the `report` job cancelled by the next push).

PO approval: **approved by Stein himself.** «Godkjent» at 15:40:25 CET (ts `1793976025.593179`) in Carl
Kristian's claim thread (ts `1793975979.776719`).

`TEAM_URL=https://fagsem-h2026-team-11.scienta.cloud`

## Security: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The security guardian reports no blocker | Ferdinand, [`workflow/security-review.md`](../security-review.md): PR #11 `76513a4` (trimmed average, 2 decimals) "OK to merge". One constant query with MIN and MAX, no division by zero, edge cases checked. | done |

## Architecture: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The architecture guardian reports no blocker | Fiffi: "OK to merge" for `76513a4` (relayed by The Hand). We found no entry for PR #11 in [`workflow/architecture-review.md`](../architecture-review.md). | done |

## Functionality: what is implemented, and is it tested OK?

Rules R1–R6 of [`workflow/specs/ratings-cr/spec.md`](../specs/ratings-cr/spec.md): with 5 or more
ratings, the average leaves out one highest and one lowest score; the average has 2 decimals with
banker's rounding; the count still counts every reviewer; the ranking uses the new average; an
unrated talk has no average.

| Check | Evidence | State |
|-------|----------|-------|
| Every rule in `spec.md` has at least one example row | R1–R6. One open question about `/admin/stats`, which no harness spec covers, so it does not block the claim. | done |
| Every example row has a Hurl request | `examples/our-ratings-cr.hurl` (`1e8572f`, "Hurl rows for R1-R6"), 354 requests. | done |
| Our examples pass | 354/354 against a local build of `f95fd45` (Fuglen, for the submission-cap run), which has the ratings-cr code. No run against the team URL. | done locally |
| The JUnit tests pass in CI | [Run 42235128898](https://github.com/Scienta/team-11/actions/runs/42235128898) for `6227fcb`: job `tests` success | done |
| The deploy workflow is green for the commit | The same run: job `deploy` success | done |
| `GET /health` on the team URL shows that SHA as `version` | `6227fcbc8037…`, read after the claim | done |
| No FAULTY check on the harness for this feature or the base features | At tick 195 the 4 ratings-cr checks had not run. `ratings-cr/check-4` was FAULTY one tick (197) under the harness's 2x load, on `PUT /talks/{id}/ratings/{reviewerId}`, and OK again at tick 198. Frode's finding: all 36 PUTs of the burst reached the app and got 200 in 13–388 ms, but 23 of them arrived about 5 s after the burst began. The wait is before the app (the platform proxy or a queue), not in our code, and not the deploy: `e18954e` switched at 14:46:18Z, after the tick. The same pattern at tick 194 (`talks/check-4`, `ratings/check-3`). | not a code defect |

When the person has claimed, copy the evidence into the claim log in `REVIEW.md` section 3.
