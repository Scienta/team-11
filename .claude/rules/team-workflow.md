# Team 11: how agents work in this repo

The steps from feature request to claim are in `workflow/process.md`. Follow them in order.

## Specs

- Save each feature request unchanged as `workflow/specs/<feature>/request.md`. Write our spec
  next to it as `spec.md`, from `workflow/spec-template.md`.
- When the request does not say which status code, edge case or error to use, decide, and write the
  decision and the reason in "Our decisions" in `spec.md`. Use the base features' behaviour as the
  default.
- Put a business-rule question for the product owner in "Open questions". Do not guess the answer.
  A person asks the product owner.
- Refer to rule IDs (R1, R2 …) in test names, Hurl comments and commit messages.

## Code

- Write the test first from the example table, and see it fail.
- The legacy code has known traps: the README is out of date, and some comments and constants do not
  match the specs. The spec on the harness is the source of truth, not the code or its comments.
- Keep each commit small enough to pass the tests on its own. Commit after the tests pass. Do not
  push: a person approves each push, because a push deploys.

## Running things on this laptop

- Run Maven (`./mvnw`) outside the sandbox. Maven must write to `~/.m2`, and the Testcontainers
  tests must reach Docker.
- Docker is Colima, linked at `/var/run/docker.sock`. The sandbox blocks the real socket behind the
  link, so run `docker` outside the sandbox. If `colima status` says it is not running, ask a
  person to run `colima start`.
- If Docker is not available, run only the tests that need no database (`-Dtest=...`) and say
  which tests did not run.
- Run the Hurl examples with `examples/run.sh https://fagsem-h2026-team-11.scienta.cloud`. This
  works inside the sandbox.
- Use `workflow/harness.sh` for the harness API. Never print, log or commit
  `.fagdag/harness-token`.
- Read the deployed service's requests with `.github/fagdag/logs.sh --since <RFC 3339 time>`. Each
  request has one line from `RequestLogFilter`.

## Evidence

- Never say that a feature is done without evidence that a person can check: a green Actions run, a
  passing `examples/run.sh` against the team URL, or a harness result.
- After a claim or a skip, add the row to the claim log in `REVIEW.md` section 3.
- When the harness finds a defect that our review did not find, add it to `REVIEW.md` section 4.
