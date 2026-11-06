# Claim evidence: submission-cap-cr

The rows come from [`../claim-checklist.md`](../claim-checklist.md). The product owner reads a claim
request in three sections: security, architecture and functionality.

Claim: **claimed at harness tick 205** by Nils Christian, at 15:53 CET. Deployed at the claim: `e7304e2`
(`GET /health`, live since 14:52:16 UTC; [deploy run 42237484511](https://github.com/Scienta/team-11/actions/runs/42237484511)
was still running at 14:53:50Z). It has the code `1362610` (the pushed copy of `f95fd45`) and the Hurl
rows `3d2975b` (the pushed copy of `caa7077`).

PO approval: **approved by Stein himself.** «Godkjent» at 15:50:15 CET (ts `1793976615.259529`) in the
claim thread (ts `1793976561.957629`), and «Begge er godkjent» in the same thread, after The Hand asked
whether both requests were approved.

`TEAM_URL=https://fagsem-h2026-team-11.scienta.cloud`

## Security: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The security guardian reports no blocker | Ferdinand, [`workflow/security-review.md`](../security-review.md), `267cb08` and `f95fd45`: OK. No lock path from speaker to talk, so no deadlock; R6 (10 at once, 3 rounds) gives exactly 1 success; 15 s of mixed load: no deadlock, no 500, nobody above 3. SQL bound. | done |

## Architecture: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The architecture guardian reports no blocker | We found no entry for `f95fd45` in [`workflow/architecture-review.md`](../architecture-review.md). | **not seen by us** |

## Functionality: what is implemented, and is it tested OK?

Rules R1–R8 of [`workflow/specs/submission-cap-cr/spec.md`](../specs/submission-cap-cr/spec.md): the
limit of 3 talks counts co-speaking; rejected and withdrawn talks do not count; a freed place can be
used at once; the limit holds under concurrent requests; nothing that exists is removed; a speaker's own
list shows only their own talks. The product owner's answer (15:36): a rejected or withdrawn talk still
gets no new co-speaker (Co-speakers R9).

| Check | Evidence | State |
|-------|----------|-------|
| Every rule in `spec.md` has at least one example row | R1–R8. The open question is answered (R9 stays, the user's decision). | done |
| Every example row has a Hurl request | `examples/our-submission-cap.hurl`, 128 requests: R1–R5 and R8. R6 (at the same time) and R7 (over the limit from before) are JUnit only: Hurl sends one request at a time, and R7 cannot be set up through the API after the change. The existing rows that the cap changes are updated: `our-acceptance.hurl`, `our-co-speakers.hurl`, `examples/local/our-scheduling.hurl`, `examples/local/our-programme-export.hurl`. | done |
| Our examples pass | Local build of `f95fd45` (Fuglen): `our-submission-cap.hurl` 128/128, all top-level files with 0 errors, local scheduling 225/225. Frikke on the candidate: Hurl 1023 + 333 + 225 requests green. | done locally |
| The JUnit tests pass | `SubmissionCapTest` (R1–R7, red first), `AcceptanceTest` R7 and `CoSpeakersTest` R7 changed. Frekken: 602 tests, 0 failures. Frikke on the candidate: 695 tests on Java 25. | done locally; **waiting**: the CI run for `e7304e2` |
| `GET /health` on the team URL shows that SHA as `version` | `e7304e24b099…` at 14:53:50Z | done |
| No FAULTY check on the harness for this feature or the base features | At tick 206: no submission-cap-cr check was not OK. 7 checks of other features were FAULTY at tick 206, all with no answer recorded, during the switch to `e7304e2`. | **watch** |

When the person has claimed, copy the evidence into the claim log in `REVIEW.md` section 3.
