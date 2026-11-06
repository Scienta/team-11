# Claim evidence: scheduling

The rows come from [`../claim-checklist.md`](../claim-checklist.md). A person claims only when no
row says "waiting" or "gap". The product owner reads a claim request in three sections: security,
architecture and functionality.

Claim: **claimed at harness tick 162** by Nils Christian. Deployed: `7bdf99a` from 13:53Z ([deploy run 42229118539](https://github.com/Scienta/team-11/actions/runs/42229118539), green),
then `dfd57c9` from 14:07:19 CET ([deploy run 42230675486](https://github.com/Scienta/team-11/actions/runs/42230675486): deploy and tests green).
Both have the scheduling code `07f7d81` (the pushed copy of `7d4d8e0`).

PO approval: **approved by Stein himself, twice.** «godkjent» at 15:07:40 CET at the top level of Slack #team-11
(ts `1793974060.960789`, from his own account; it did not name the feature, but the only open request was
the scheduling overview, ts `1793973986.734579`), and «Godkjent» in the scheduling thread at 15:08:33 CET
(ts `1793974113.663289`), right after the requirement-by-requirement overview.

`TEAM_URL=https://fagsem-h2026-team-11.scienta.cloud`

## Security: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The security guardian reports no blocker | Ferdinand, [`workflow/security-review.md`](../security-review.md), review of `7d4d8e0`: no blocker. The SQL is bound, the slot lock and the unique indexes hold R8 under parallel PUTs, and bad input gives 400. Note S10b (low): anybody can put talks on the programme and take them out, because the spec has no auth model. That is the same product owner question as S10, not a defect. | done |

## Architecture: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The architecture guardian reports no blocker | Fiffi, [`workflow/architecture-review.md`](../architecture-review.md): `07f7d81` fits P1 and P3–P8, and it locks in one order only (the slot `FOR UPDATE`, then the talk `FOR SHARE`). No blocker. 4 "tidy after" items: G10 (a sealed `TakeOutOutcome`) done in `aaac95a`, G11 (`addUnlessThere`) done in `a223c14`, G12 (a `TalkFormat` enum) and G13 (the `TalkStatus` constructor) open. | done |

## Functionality: what is implemented, and is it tested OK?

Rules R1–R8 of [`workflow/specs/scheduling/spec.md`](../specs/scheduling/spec.md): the fixed rooms
and slots, putting an accepted talk in a place, looking it up and taking it out, a workshop only in
the long slot, one talk per place and each talk once, no speaker in two rooms in the same slot,
later changes that leave the programme as it is, and several committee members at the same time.

| Check | Evidence | State |
|-------|----------|-------|
| Every rule in `spec.md` has at least one example row | R1–R8, each with an example table. Open questions: none. The overview of gaps is a later wish (our decisions). | done |
| Every example row has a Hurl request | `examples/local/our-scheduling.hurl` (`c668b8b`, pushed as `7e63da7`), 215 requests: every row of R1–R7. The R8 rows (at the same time) are JUnit only, because Hurl sends one request at a time. | done |
| Our examples pass | **Local build only**, by our decision (spec, "Our decisions": each group empties the places it uses, and on the team URL that can delete the harness's entries). Against a local build of `7d4d8e0`: 215/215 three times, also with a place taken before the run; all 15 places were empty afterwards. A copy that expects 200 for a workshop in `S1` failed, so the file can fail. 215/215 again on a local build of `b700b6e`. | done |
| The JUnit tests pass | `SchedulingTest` 78/78, the R8 rows at the same time included (49/49 table rows in `docs/specs/programme-scheduling`). CI: [run 42229118539](https://github.com/Scienta/team-11/actions/runs/42229118539) for `7bdf99a`, job `tests` success. | done |
| The deploy workflow is green for the commit | [Run 42229118539](https://github.com/Scienta/team-11/actions/runs/42229118539) for `7bdf99a`: job `deploy` success. Then [run 42230675486](https://github.com/Scienta/team-11/actions/runs/42230675486) for `dfd57c9`: job `deploy` success. | done |
| `GET /health` on the team URL shows that SHA as `version` | `7bdf99a5e8b8…` at 14:05:52Z. `dfd57c973cdc…` after 14:07:19 CET. | done |
| A read-only check on the team URL | At 14:05:52Z on `7bdf99a` (Fuglen): `GET /programme/R1/S1`, `R2/S3` and `R3/S5` gave 200, and `GET /programme/R4/S1` gave 400. No PUT and no DELETE on the team URL. | done |
| The base features still pass their examples | The harness checks of the base features stayed OK (below). The last run of our base examples on the team URL was at 13:39:51Z–13:41:50Z on `3318bf1`: all passed. | done |
| No FAULTY check on the harness for this feature or the base features | `workflow/harness.sh feedback` at tick 161: all 24 claimed checks OK (speakers 4, talks 6, ratings 5, acceptance 5, workshop-registration 4), 732.05 points. The harness has no checks for scheduling until it is claimed. After the claim: 6 scheduling checks, not run yet at tick 162; we watch them. | done before the claim |

When the person has claimed, copy the evidence into the claim log in `REVIEW.md` section 3.
