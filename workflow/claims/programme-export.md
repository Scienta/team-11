# Claim evidence: programme-export

The rows come from [`../claim-checklist.md`](../claim-checklist.md). The product owner reads a claim
request in three sections: security, architecture and functionality.

Claim: **claimed at harness tick 206** by Nils Christian, at 15:53 CET. Deployed at the claim: `e7304e2`
(`GET /health`, live since 14:52:16 UTC; [deploy run 42237484511](https://github.com/Scienta/team-11/actions/runs/42237484511)
was still running at 14:53:50Z). It has the export code (the pushed copy of `b700b6e`) and the Hurl file
`04f81dd`, with the R4 row under the submission cap in `d094bf3`.

PO approval: **approved by Stein himself.** «Godkjent» at 15:50:15 CET (ts `1793976615.259529`) in the
claim thread (ts `1793976561.957629`), and «Begge er godkjent» in the same thread, after The Hand asked
whether both requests were approved.

`TEAM_URL=https://fagsem-h2026-team-11.scienta.cloud`

## Security: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The security guardian reports no blocker | Ferdinand, [`workflow/security-review.md`](../security-review.md), `b700b6e`: no blocker. RFC 4180 quoting round-trips titles and speaker names, `text/csv` for every `Accept` header, read-only, 7 ms. Low: S14 (no formula guard in `/programme.csv`, decided by the request) and S16 (a title with U+2028 and similar characters is not quoted). | done |

## Architecture: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The architecture guardian reports no blocker | [`workflow/architecture-review.md`](../architecture-review.md), programme export `b700b6e`: no blocker. `Programme` and `ProgrammeCsv` are pure, one read-only query. Tidy after: G22 (the order of the slots), G23 (`ProgrammeRow.format` is a `String`). | done |

## Functionality: what is implemented, and is it tested OK?

Rules R1–R7 of [`workflow/specs/programme-export/spec.md`](../specs/programme-export/spec.md): the whole
programme as JSON and as CSV, in the order of a day, each row as the talk is now, the speakers by
name, a CSV that is UTF-8 and standard RFC 4180, and an empty programme.

| Check | Evidence | State |
|-------|----------|-------|
| Every rule in `spec.md` has at least one example row | R1–R7. Open questions: none. | done |
| Every example row has a Hurl request | `examples/local/our-programme-export.hurl`, 333 requests: every row of R1–R7, except "one moment in time" and "a missing talk or speaker row", which need a database set-up (JUnit). | done |
| Our examples pass | **Local build only**, because the file empties the shared programme. `b700b6e`: 332/332 three times, also with a place taken before the run. `f95fd45` + `b700b6e` (with the submission cap): 333/333 twice. Frikke on the candidate: 333/333. | done locally |
| The JUnit tests pass | 38/38 table rows in `docs/specs/programme-export*`. Frikke on the candidate: 695 tests on Java 25. | done locally; **waiting**: the CI run for `e7304e2` |
| `GET /health` on the team URL shows that SHA as `version` | `e7304e24b099…` at 14:53:50Z | done |
| No FAULTY check on the harness for this feature or the base features | At tick 206 the 3 programme-export checks had not run. 7 checks of other features were FAULTY at tick 206, all with no answer recorded, during the switch to `e7304e2`. | **watch** |

When the person has claimed, copy the evidence into the claim log in `REVIEW.md` section 3.
