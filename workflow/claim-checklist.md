# Claim checklist

A claim is permanent. A person claims only when each line below has evidence that another person can
check. Copy the evidence into the claim log in `REVIEW.md` section 3.

Set the team URL first: `TEAM_URL=https://fagsem-h2026-team-11.scienta.cloud`

| Check | Evidence |
|-------|----------|
| Every rule in `spec.md` has at least one example row | Link to the spec |
| Every example row has a Hurl request | `examples/our-<feature>.hurl` |
| The JUnit tests pass in CI | Link to the Actions run |
| The deploy workflow is green for the commit | Link to the Actions run, and the SHA |
| `GET /health` on the team URL shows that SHA as `version` | `curl -s "$TEAM_URL/health"` |
| `examples/run.sh "$TEAM_URL"` passes, all files | Output of the run |
| The review agent found no open defect | The review summary |
| The base features still pass their examples | Output of the run |
| No FAULTY check on the harness for this feature or the base features | `workflow/harness.sh feedback` |

If one line has no evidence, do not claim. Write "skipped" or "waiting" in the claim log, with the
reason.
