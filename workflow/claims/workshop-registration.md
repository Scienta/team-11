# Claim evidence: workshop-registration

The rows come from [`../claim-checklist.md`](../claim-checklist.md). A person claims only when no
row says "waiting" or "gap". The product owner reads a claim request in three sections: security,
architecture and functionality.

Claim: **claimed at harness tick 136** by Nils Christian. Deployed: `c4fe79c` ([deploy run 42226345319](https://github.com/Scienta/team-11/actions/runs/42226345319), green),
then `3318bf1` from 13:39:51Z (`GET /health`; [deploy run 42226853638](https://github.com/Scienta/team-11/actions/runs/42226853638) was still running).
Both have the same workshop code and the same `examples/our-workshop-registration.hurl`: unregistering
with the email (R10 as of `9573d72`). The cancellation token (`b8b5077`) is not deployed.

PO approval: **approved by Ingvild Hardeng on Stein's behalf**: "Jeg godkjenner for Stein", at the top level of Slack
#team-11, 14:42:57 CET (13:42:57Z), after the claim request (ts `1793972501.624669`). Stein himself has not
answered in the claim thread.

**Correction (14:51–14:53):** Stein objected in Slack at 14:51:33 CET that he gave nobody the authority to
approve for him. Nils Christian answered in Stein's thread at 14:53: it was our mistake to accept
Ingvild's approval on his behalf, and from now on we wait for Stein's own «godkjent». The claim at tick
136 was made without the product owner's own approval. A claim is permanent, so it stays.

`TEAM_URL=https://fagsem-h2026-team-11.scienta.cloud`

## Security: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The security guardian reports no blocker | Ferdinand, [`workflow/security-review.md`](../security-review.md): S1–S12 closed. S5 (anybody can unregister anybody) closed. S5b (the email for R10 is public in the list, so anybody can read it and unregister an attendee) is fixed in `b8b5077`, the cancellation token, which is **not deployed** yet. S5c (the token mask allows only spaces) is low and open on `b8b5077`. | **open on the deployed code**: S5b, until `b8b5077` is deployed |

## Architecture: tested and OK?

| Check | Evidence | State |
|-------|----------|-------|
| The design review found nothing before the claim | Frode's design review of `3121e67` ([`workflow/architecture-review.md`](../architecture-review.md)): W1, W4 and W6 done. W3 half done (a "tidy after" item). | done |
| The architecture guardian reports no blocker | Fiffi, [`workflow/architecture-review.md`](../architecture-review.md): no blocker | done |

## Functionality: what is implemented, and is it tested OK?

Rules R1–R10 of [`workflow/specs/workshop-registration/spec.md`](../specs/workshop-registration/spec.md):
registration for an accepted workshop with a name and an email, 20 seats, one registration for each
email, the list in registration order, and unregistering, which frees the seat. R1–R8 come from the
feature request. R9 and R10 come from the product owner's answers. The deployed code proves R10 with
the email. The spec now asks for a cancellation token (`b8b5077`, not deployed).

| Check | Evidence | State |
|-------|----------|-------|
| Every rule in `spec.md` has at least one example row | R1–R10, each with an example table. Open questions: none (product owner, 2026-11-06 14:00–14:01). | done |
| Every example row has a Hurl request | `examples/our-workshop-registration.hurl` as deployed (`c4fe79c` = `3318bf1`), 211 requests: all rows of R1–R10 (email version), except the rows "at the same time" of R5 and R6. Those are JUnit only (`WorkshopRegistrationTest`), because Hurl sends one request at a time. The token version is `9345a37`, 213 requests: 213/213 against a local build of `b8b5077`, and no token in the app log. | done for the deployed code |
| The JUnit tests pass in CI | [Actions run 42226345319](https://github.com/Scienta/team-11/actions/runs/42226345319), commit `c4fe79c`: job `tests` success | done |
| The deploy workflow is green for the commit | [Actions run 42226345319](https://github.com/Scienta/team-11/actions/runs/42226345319) for `c4fe79c`: job `deploy` success | done |
| `GET /health` on the team URL shows that SHA as `version` | `c4fe79cc5257…` at 13:39:37Z. `3318bf1c2a48…` at 13:39:51Z, before the run, and at 13:40:21Z, after it. | done |
| Our examples pass on the team URL | One run at 13:39:51Z (Fuglen). `git show c4fe79c:examples/our-workshop-registration.hurl`: 211/211 in 26.9 s. The version did not change during the run. | done |
| The review agent found no open defect | Frekken's spec review of `3121e67` against `spec.md` and `review-rubric.md` items 1–7: no open defect. 3 notes, closed in `2da3f58`, `ae718e8` and `851a206`. Frode's design review: see Architecture. | done |
| The base features still pass their examples | The same run, files from `c4fe79c`: `speakers.hurl` 4/4, `talks.hurl` 5/5, `our-speakers.hurl` 14/14, `our-ratings.hurl` 16/16. Also `our-speakers.hurl` from `3318bf1`, 18/18, at 13:40:45Z. `ratings.hurl` did not run: it has an unfiltered `GET /talks?sort=rating`, and we limit those on the team URL. | done |
| No FAULTY check on the harness for this feature or the base features | `workflow/harness.sh feedback` at tick 133: all 20 checks OK (speakers 4, talks 6, ratings 5, acceptance 5), 550.05 points. The harness has no checks for workshop-registration until it is claimed. | done for the claimed features |

| After the claim: the workshop-registration checks on the harness | `workflow/harness.sh feedback` at tick 136: check-1 to check-4 not run yet (status `null`). The other 20 checks OK, 568.05 points. | **waiting**: the first results; we watch them |

When the person has claimed, copy the evidence into the claim log in `REVIEW.md` section 3.
