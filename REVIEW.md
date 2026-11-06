# REVIEW

> **Showcase brief (17:00, 4 minutes, hard stop).** You screen-share this repo from the shared
> presenter laptop, in a random order drawn at 17:00. No slides, and no harness screens.
>
> 1. **Our workflow in one minute:** section 1, pointing at the files in `workflow/` and `.claude/`.
> 2. **One story:** a bug our review caught before the harness did, or one the harness caught that
>    we missed, and why. Use the claim log.
> 3. **One artifact we'd reuse**, opened on screen.
>
> Keep this file as a running log from t = 0, so 17:00–17:05 is for tidying, not writing. To be
> eligible to win, every heading below needs some text. The comments under each heading are prompts
> and don't count; replace them with your own words.

## 1. Workflow

<!-- The steps you actually ran, from spec to claim. Link to the files in workflow/ and .claude/. -->

Draft from 12:30. We update it when the steps change.

1. Fetch the feature request with [`workflow/harness.sh`](workflow/harness.sh). Write our spec
   from [`workflow/spec-template.md`](workflow/spec-template.md): rules with IDs, our decisions on
   status codes and edge cases, and an example table for each rule.
2. A person takes the open questions to the product owner.
3. Hurl examples and JUnit tests come from the example tables, before the code.
4. A review agent checks the diff against the spec with
   [`workflow/review-rubric.md`](workflow/review-rubric.md).
5. A person approves the push. We verify on the team URL with the Hurl examples, the request log
   (`RequestLogFilter`, read with `.github/fagdag/logs.sh`) and the harness feedback.
6. A person claims only after [`workflow/claim-checklist.md`](workflow/claim-checklist.md) has
   evidence on every line.

The full steps are in [`workflow/process.md`](workflow/process.md). The agent rules are in
[`.claude/rules/team-workflow.md`](.claude/rules/team-workflow.md).

## 2. How we reviewed agent output

<!-- Who or what reviewed the agents' work (people, review agents, tests, Hurl examples, gates in
the deploy workflow), and against what (the spec, its examples, your own checks). -->

## 3. Claim log

<!-- A running entry per feature or change request, written when you claim or decide to skip it:
claimed or skipped, at which tick, and the evidence you held before claiming. For example:

| Feature | Claimed or skipped | Tick | Evidence before claiming |
|---|---|---|---|
| Speakers | claimed | 3 | examples.hurl green against our URL, plus our own edge-case tests for R3 and R7 |
-->

## 4. What the harness caught that we didn't

<!-- Failures the harness found after you claimed (or after a deploy), and why your own review missed them. -->

## 5. Reusable artifacts

<!-- What's worth taking to client work: agents, skills, prompts, spec templates, gates. Give the paths. -->

## 6. One thing we'd do differently

<!-- One thing, and why. -->
