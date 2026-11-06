# Claim evidence: scheduling

The rows come from [`../claim-checklist.md`](../claim-checklist.md). A person claims only when no
row says "waiting" or "gap". The product owner reads a claim request in three sections: security,
architecture and functionality.

Claim: **waiting for the person.** To deploy: `ff989b7` (`origin/main`). It has the scheduling code
`07f7d81` (the pushed copy of `7d4d8e0`) and the Hurl file `7e63da7` (the pushed copy of `c668b8b`).
Deployed at 13:45Z: `3318bf1`, without scheduling.

PO approval: **waiting**.

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
| Our examples pass | **Local build only**, by our decision (spec, "Our decisions": each group empties the places it uses, and on the team URL that can delete the harness's entries). Against a local build of `7d4d8e0`: 215/215 three times, also with a place taken before the run; all 15 places were empty afterwards. A copy that expects 200 for a workshop in `S1` failed, so the file can fail. | done |
| The JUnit tests pass | `SchedulingTest` 78/78, the R8 rows at the same time included. | done locally; **waiting**: the CI run for `ff989b7` |
| The deploy workflow is green for the commit | | **waiting**: the Actions run for `ff989b7` |
| `GET /health` on the team URL shows that SHA as `version` | | **waiting**: Filla reports the deploy |
| A read-only check on the team URL | `GET /programme/R1/S1` and two more places (200 or 404), and `GET /programme/R4/S1` (400). No PUT and no DELETE on the team URL. | **waiting**: after the deploy |
| The base features still pass their examples | | **waiting**: after the deploy |
| No FAULTY check on the harness for this feature or the base features | At tick 136: the 20 checks of speakers, talks, ratings and acceptance OK; the 4 workshop-registration checks not run yet. The harness has no checks for scheduling until it is claimed. | **waiting**: after the deploy |

When the person has claimed, copy the evidence into the claim log in `REVIEW.md` section 3.
