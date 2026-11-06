# Process: from feature request to claim

We follow these steps for every feature. A person on the team owns each step that has "person" in it.
An agent does the other steps.

1. **Fetch the spec.** Run `workflow/harness.sh spec <feature>`. Save the output as
   `workflow/specs/<feature>/request.md`. Do not edit that file.
2. **Write our spec.** Copy `workflow/spec-template.md` to `workflow/specs/<feature>/spec.md`. Fill in
   the rules, the API, our decisions (status codes, edge cases, errors) and an example table for each
   rule.
3. **Ask the product owner (person).** A person takes the open questions to the organisers' product
   owner. Write each answer into the spec with the time of the answer.
4. **Write the Hurl examples.** Put one Hurl file per feature in `examples/<feature>.hurl`. It covers
   every row in the example tables, not only the happy path.
5. **Write the tests first.** Write JUnit tests from the example tables. See them fail.
6. **Implement.** Make the tests pass. Commit each step that compiles and passes the tests.
7. **Review.** A review agent compares the diff with `spec.md`, row by row. It does not see our
   reasoning, only the spec and the code. See `workflow/review-rubric.md`.
8. **Deploy (person approves the push).** Push to `main`. Wait for the deploy workflow to go green.
9. **Verify on the deployed service.** Run `examples/run.sh https://fagsem-h2026-team-11.scienta.cloud`. Read the
   request log with `.github/fagdag/logs.sh` and the harness feedback with
   `workflow/harness.sh feedback`.
10. **Claim (person).** Go through `workflow/claim-checklist.md`. If every box has evidence, a person
    claims on the harness. Write the entry in the claim log in `REVIEW.md` section 3.

## When a harness check is FAULTY

1. Run `workflow/harness.sh timeline <feature>/<check>` to get the transcript.
2. Find the same requests in our request log.
3. Add the failed case as a new row in the example table of `spec.md`, then as a Hurl request and a
   test. See the test fail before you fix the code.
4. Write the case in `REVIEW.md` section 4: what the check found, and why our review did not find it.
