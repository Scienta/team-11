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

Four checks, in this order. Each one checks against the spec on the harness, not against the code.

1. **Tests first, from the example tables.** Each row in `workflow/specs/<feature>/spec.md` becomes a
   JUnit row before the code changes, and we see it fail.
2. **A review agent with no shared context.** It gets the harness spec, our spec, the rubric
   ([`workflow/review-rubric.md`](workflow/review-rubric.md)) and the commits. It does not get the
   conversation that wrote the code.
3. **Hurl examples against the real service.** `examples/our-*.hurl` cover every example row. We run
   them against a local build first, then against the team URL after the deploy.
4. **The request log and the harness transcripts.** `RequestLogFilter` writes one line per request,
   so we can compare our answers with what the checks sent.

Story (12:50): the Hurl run against a local build failed on `ratedAtDisplay`. The local JDK is 27,
and from Java 20 the US short format puts U+202F, not a space, before "AM". The service runs Java
11, so the harness passed, and no test checked the format. We fixed it with an explicit pattern
before a Java upgrade could break it (`f9fa516`).

## 3. Claim log

<!-- A running entry per feature or change request, written when you claim or decide to skip it:
claimed or skipped, at which tick, and the evidence you held before claiming. For example:

| Feature | Claimed or skipped | Tick | Evidence before claiming |
|---|---|---|---|
| Speakers | claimed | 3 | examples.hurl green against our URL, plus our own edge-case tests for R3 and R7 |
-->

| Feature | Claimed or skipped | Tick | Evidence before claiming |
|---|---|---|---|
| Speakers, talks, ratings | claimed by the organisers | 0 | Base features. At 12:25 all 15 checks were OK. Our own review then found 3 gaps against the specs (email, bio length, reviewer range), fixed in `336589f`, `fa1480e`, `dbe89fc`. |

## 4. What the harness caught that we didn't

<!-- Failures the harness found after you claimed (or after a deploy), and why your own review missed them. -->

## 5. Reusable artifacts

<!-- What's worth taking to client work: agents, skills, prompts, spec templates, gates. Give the paths. -->

## 6. One thing we'd do differently

<!-- One thing, and why. -->
