# Claim evidence: acceptance

The rows come from [`../claim-checklist.md`](../claim-checklist.md). A person claims only when no
row says "waiting" or "gap". The product owner reads a claim request in three sections: security,
architecture and functionality.

Claim: **claimed at harness tick 124** by Nils Christian. Deployed commit: `c43bbaf` (`origin/main`, `GET /health` at 13:27:14Z).

PO approval: **approved.** Stein answered «Godkjent» at 14:30:30 CET (13:30:30Z) in Slack #team-11
(ts `1793971830.749579`). The Hand asked him at 13:59 CET (ts `1793969993.259739`).

`TEAM_URL=https://fagsem-h2026-team-11.scienta.cloud`

## Security: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The security guardian reports no blocker | [`workflow/security-review.md`](../security-review.md), the report for the commits of this feature | **waiting**: the guardian's report |

## Architecture: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The design review found nothing before the claim | Design review by Frikke (`review-rubric.md` item 8): nothing before the claim. 9 "tidy after" items, done in `da1b44f`..`b6c65cc` (local, not pushed yet). | done |
| The architecture guardian reports no blocker | [`workflow/architecture-review.md`](../architecture-review.md), the report for the commits of this feature | **waiting**: the guardian's report |

## Functionality: what is implemented, and is it tested OK?

Rules R1–R9 of [`workflow/specs/acceptance/spec.md`](../specs/acceptance/spec.md): the committee's
decision on a talk, the speaker's withdrawal, the status in every place that shows a talk, and
withdrawn talks last in the lists.

| Check | Evidence | State |
|-------|----------|-------|
| Every rule in `spec.md` has at least one example row | Rules R1–R9. The R5 example table came in `6146f98`. Open questions: none, the product owner answered in Slack #team-11. | done |
| Every example row has a Hurl request | `examples/our-acceptance.hurl`, 189 requests, the same file on `origin/main` and in the working tree. The run against the team URL (below) passed it. | done |
| The JUnit tests pass in CI | [Actions run 42224526394](https://github.com/Scienta/team-11/actions/runs/42224526394), commit `c43bbaf`: job `tests`, step "Unit tests" success | done |
| The deploy workflow is green for the commit | [Actions run 42224526394](https://github.com/Scienta/team-11/actions/runs/42224526394) for `c43bbaf`: job `deploy` success | done |
| `GET /health` on the team URL shows that SHA as `version` | `curl -s "$TEAM_URL/health"`: `version` `c43bbaffad4d…` at 13:27:14Z | done |
| `examples/run.sh "$TEAM_URL"` passes, all files | One run at 13:27:33Z on `c43bbaf` (Fuglen): `our-acceptance.hurl` 189/189. 7 of 8 files passed, 278 requests. The one failure is `our-workshop-registration.hurl`: workshop registration is not deployed yet (404 at its first registration). | done for this feature |
| The spec review found no open defect | Spec review by Fiffi (`review-rubric.md` items 1–7): no code defect. 4 test gaps, closed in `6146f98` and checked again. | done |
| The base features still pass their examples | The same run: `speakers.hurl`, `talks.hurl`, `ratings.hurl`, `our-speakers.hurl` and `our-ratings.hurl` passed. | done |
| No FAULTY check on the harness for this feature or the base features | `workflow/harness.sh feedback`: tick 121, all 15 checks OK, 484.05 points. The 15 checks are the base features only (speakers 4, talks 6, ratings 5). The harness has no checks for acceptance until it is claimed: the timeline for `acceptance/check-1` answers 404 "No such check among your claimed features". | done for the base features |
| Claim risk: response times on the team URL | Before the claim, an unfiltered `GET /talks` took 12–17 s on `c43bbaf`. Closed by `88e5d86` on `3318bf1`: 418 ms for `GET /talks` and 284 ms for `GET /talks?sort=rating` at 13:41:50Z. | closed |
| After the claim: the acceptance checks on the harness | `workflow/harness.sh feedback` at tick 125: acceptance check-1 to check-5 all OK, and the 15 base checks OK. 502.05 points. | OK at tick 125; we watch them |

When the person has claimed, copy the evidence into the claim log in `REVIEW.md` section 3.
