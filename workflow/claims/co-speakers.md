# Claim evidence: co-speakers

The rows come from [`../claim-checklist.md`](../claim-checklist.md). A person claims only when no
row says "waiting" or "gap". The product owner reads a claim request in three sections: security,
architecture and functionality.

Claim: **waiting for the person.** Deployed commit: `3318bf1` (`GET /health` at 13:41:50Z and 13:42:00Z). It has the R4 and R9 Hurl rows (`c8e30de`, the pushed copy of `ec38273`) and the ratings-query fix `88e5d86`.
Co-speakers came from Carl Kristian's team (PR #4, merged in `b729a5e`).

PO approval: **waiting**.

`TEAM_URL=https://fagsem-h2026-team-11.scienta.cloud`

## Security: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The security guardian reports no blocker | [`workflow/security-review.md`](../security-review.md): "Co-speakers is reviewed by Carl Kristian's team." | **waiting**: the report for the co-speakers commits |

## Architecture: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The architecture guardian reports no blocker | [`workflow/architecture-review.md`](../architecture-review.md): G2 (`CoSpeakerService.UNCHANGEABLE_STATUSES` outside `TalkStatus`) is "tidy after", not a blocker | **waiting**: the guardian's report |

## Functionality: what is implemented, and is it tested OK?

Rules R1–R10 of [`workflow/specs/co-speakers/spec.md`](../specs/co-speakers/spec.md): up to two
co-speakers on a talk, added and removed again, shown wherever a talk is shown, and no change to a
withdrawn or rejected talk.

| Check | Evidence | State |
|-------|----------|-------|
| Every rule in `spec.md` has at least one example row | Rules R1–R10, each with an example table. Open questions: none, the product owner answered in Slack #team-11 (13:11–13:14). | done |
| Every example row has a Hurl request | `examples/our-co-speakers.hurl` on `3318bf1`, 66 requests, covers every example row of R1–R9. The R9 table and the R4 rows for the answers of `PUT /talks/{id}/status` and `POST /talks/{id}/withdraw` and for `GET /talks?includeWithdrawn=false` came in `c8e30de`. R10 (at the same time) is JUnit only, because Hurl sends one request at a time. | done |
| The JUnit tests pass in CI | [Actions run 42226853638](https://github.com/Scienta/team-11/actions/runs/42226853638), commit `3318bf1`: job `tests` success | done |
| The deploy workflow is green for the commit | [Actions run 42226853638](https://github.com/Scienta/team-11/actions/runs/42226853638) for `3318bf1`: job `deploy` success | done |
| `GET /health` on the team URL shows that SHA as `version` | `3318bf1c2a48…` at 13:41:50Z, before the run, and at 13:42:00Z, after it | done |
| Our examples pass on the team URL | One run at 13:41:50Z (Fuglen), files from `3318bf1`: `our-co-speakers.hurl` 66/66 in 6.6 s. The earlier run at 13:27:33Z on `c43bbaf` passed the 36 requests of the file before `c8e30de`. | done |
| The review agent found no open defect | | **waiting**: a spec review of the co-speakers code against `spec.md` |
| The base features still pass their examples | `ratings.hurl` from `3318bf1` 8/8 at 13:41:50Z. `speakers.hurl`, `talks.hurl`, `our-speakers.hurl` (18) and `our-ratings.hurl` passed on the same deploy at 13:39:51Z–13:40:45Z. | done |
| No FAULTY check on the harness for this feature or the base features | `workflow/harness.sh feedback` at tick 133: all 20 checks OK (speakers 4, talks 6, ratings 5, acceptance 5), 550.05 points. The harness has no checks for co-speakers until it is claimed. | done for the claimed features |
| Claim risk: response times on the team URL | Closed by `88e5d86` on `3318bf1`: in the 13:41:50Z run, an unfiltered `GET /talks` took 418 ms and `GET /talks?sort=rating` 284 ms (before: 12–17 s). | closed |

When the person has claimed, copy the evidence into the claim log in `REVIEW.md` section 3.
