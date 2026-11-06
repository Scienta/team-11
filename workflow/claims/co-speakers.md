# Claim evidence: co-speakers

The rows come from [`../claim-checklist.md`](../claim-checklist.md). A person claims only when no
row says "waiting" or "gap". The product owner reads a claim request in three sections: security,
architecture and functionality.

Claim: **waiting for the person.** Deployed commit: `c43bbaf` (`origin/main`, `GET /health` at 13:27:14Z).
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
| Every example row has a Hurl request | `examples/our-co-speakers.hurl` on `c43bbaf`, 36 requests, covers the rows of R1–R8 except two R4 rows. Missing: the R9 table (a withdrawn or rejected talk gives 409, the other statuses 200), and the R4 rows for the answers of `PUT /talks/{id}/status` and `POST /talks/{id}/withdraw` and for `GET /talks?includeWithdrawn=false`. Fuglen wrote these rows from the spec: 66/66 requests against a local build of `c43bbaf`, not committed yet. Carl Kristian's team decides who commits them. R10 (at the same time) is JUnit only, because Hurl sends one request at a time. | **gap**: until the rows are committed and deployed |
| The JUnit tests pass in CI | [Actions run 42224526394](https://github.com/Scienta/team-11/actions/runs/42224526394), commit `c43bbaf`: job `tests`, step "Unit tests" success | done |
| The deploy workflow is green for the commit | [Actions run 42224526394](https://github.com/Scienta/team-11/actions/runs/42224526394) for `c43bbaf`: job `deploy` success | done |
| `GET /health` on the team URL shows that SHA as `version` | `curl -s "$TEAM_URL/health"`: `version` `c43bbaffad4d…` at 13:27:14Z | done |
| `examples/run.sh "$TEAM_URL"` passes, all files | One run at 13:27:33Z on `c43bbaf` (Fuglen): `our-co-speakers.hurl` 36/36. 7 of 8 files passed, 278 requests. The one failure is `our-workshop-registration.hurl`: workshop registration is not deployed yet. | done for this feature |
| The review agent found no open defect | | **waiting**: a spec review of the co-speakers code against `spec.md` |
| The base features still pass their examples | The same run: `speakers.hurl`, `talks.hurl`, `ratings.hurl`, `our-speakers.hurl` and `our-ratings.hurl` passed. | done |
| No FAULTY check on the harness for this feature or the base features | `workflow/harness.sh feedback`: tick 121, all 15 checks OK, 484.05 points, base features only. The harness has no checks for co-speakers until it is claimed: the timeline for `co-speakers/check-1` answers 404 "No such check among your claimed features". | done for the base features |
| Claim risk: response times on the team URL | An unfiltered `GET /talks` takes 12–17 s on `c43bbaf` (4017 talks, 1.1 MB). Co-speakers R4 shows `coSpeakerIds` in every list, so a harness check that lists all talks can time out after the claim. Frekken's ratings query fixes it in the next push. | **risk**: until that push is deployed |

When the person has claimed, copy the evidence into the claim log in `REVIEW.md` section 3.
