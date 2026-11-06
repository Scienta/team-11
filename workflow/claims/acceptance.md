# Claim evidence: acceptance

The rows come from [`../claim-checklist.md`](../claim-checklist.md). A person claims only when no
row says "waiting" or "gap". The product owner reads a claim request in three sections: security,
architecture and functionality.

Claim: **waiting for the person.** Deployed commit: `3a02708`.

PO approval: **waiting**. The product owner, Stein, approves claims. The Hand asked him at 13:59 CET (12:59:53Z)
(Slack #team-11, ts `1793969993.259739`).

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
| Every example row has a Hurl request | `examples/our-acceptance.hurl` in `3f64c98`, 189 requests. The run against the team URL (below) passed the committed file. | done |
| The JUnit tests pass in CI | [Actions run 42220804835](https://github.com/Scienta/team-11/actions/runs/42220804835), commit `3e7a9b7` | done |
| The deploy workflow is green for the commit | [Actions run 42220804835](https://github.com/Scienta/team-11/actions/runs/42220804835) for `3e7a9b7`, then [run 42221404330](https://github.com/Scienta/team-11/actions/runs/42221404330) for `3a02708` (docs only) | done |
| `GET /health` on the team URL shows that SHA as `version` | `curl -s "$TEAM_URL/health"`: `version` `3a027087` at 12:57:50Z | done |
| `examples/run.sh "$TEAM_URL"` passes, all files | 6/6 files, 236/236 requests, 0 failures (Ferdinand, 12:57:50Z) | done |
| The spec review found no open defect | Spec review by Fiffi (`review-rubric.md` items 1–7): no code defect. 4 test gaps, closed in `6146f98` and checked again. | done |
| The base features still pass their examples | The same run: `speakers.hurl`, `talks.hurl` and `ratings.hurl` are 3 of the 6 files. | done |
| No FAULTY check on the harness for this feature or the base features | `workflow/harness.sh feedback`: tick 91, all 15 checks OK, 364.55 points. The 15 checks are the base features only (speakers 4, talks 6, ratings 5). Acceptance has no checks until it is claimed. | done for the base features |

When the person has claimed, copy the evidence into the claim log in `REVIEW.md` section 3.
