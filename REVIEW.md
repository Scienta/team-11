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
4. Two reviews for each batch: a spec review and a design review, with
   [`workflow/review-rubric.md`](workflow/review-rubric.md). Then the guardians review the batch
   (below).
5. Frikke builds the push candidate in its own git worktree, runs the full suite there and makes
   the report. A person approves the push, because a push deploys. We verify on the team URL with
   the Hurl examples, the request log (`RequestLogFilter`, read with `.github/fagdag/logs.sh`) and
   the harness feedback.
6. A person claims only after [`workflow/claim-checklist.md`](workflow/claim-checklist.md) has
   evidence on every line, and the product owner has approved the claim.

The full steps are in [`workflow/process.md`](workflow/process.md). The agent rules are in
[`.claude/rules/team-workflow.md`](.claude/rules/team-workflow.md).

**One coordinator and three guardians.** About 10 Claude sessions work on one laptop, in one working
tree. The rules are in [`workflow/coordination.md`](workflow/coordination.md).

| Role | What it does | File |
|---|---|---|
| Coordinator ("The Hand") | Takes all requests from the person, the agent sessions and Slack. Gives each task to an agent session. Changes no files. | [`workflow/coordination.md`](workflow/coordination.md) |
| Security guardian (Ferdinand) | Reviews each batch before a push against checks C1–C8, and runs the exploit script against a local build. Writes no fixes. | [`workflow/security-review.md`](workflow/security-review.md) |
| Architecture guardian (Fiffi) | Reviews each batch against our patterns P1–P8. Writes no code. | [`workflow/architecture-review.md`](workflow/architecture-review.md) |
| Functionality guardian (Fuglen) | Collects the claim evidence for each feature, line by line from the claim checklist. | [`workflow/claims/`](workflow/claims/) |
| Watchers (Filla, Foffo) | Filla watches the deploy, the request log and the harness, and posts alerts. Foffo reads Slack and reports new messages to The Hand. | [`workflow/coordination.md`](workflow/coordination.md) |
| Push candidates (Frikke) | Builds each push candidate in its own git worktree, runs the full suite and makes the report. | [`workflow/process.md`](workflow/process.md) |
| Agent sessions | Write the specs, tests and code. Each session tests in its own git worktree and commits only its own files. | [`.claude/rules/team-workflow.md`](.claude/rules/team-workflow.md) |

- **Claims in Slack.** The Hand claims each task in Slack #team-11 before it gives the task to a
  session. Carl Kristian's laptop runs agents too, and the claim stops two laptops from doing the
  same work.
- **Product owner approval in three parts.** A claim request has three sections: security,
  architecture and functionality, each with evidence that a person can check. The product owner
  approves in Slack before a person presses claim. Example: acceptance, «Godkjent» at 14:30:30 CET,
  in [`workflow/claims/acceptance.md`](workflow/claims/acceptance.md).

## 2. How we reviewed agent output

<!-- Who or what reviewed the agents' work (people, review agents, tests, Hurl examples, gates in
the deploy workflow), and against what (the spec, its examples, your own checks). -->

Six checks, in this order. The functional checks compare with the spec on the harness, not with the
code.

1. **Tests first, from the example tables.** Each row in `workflow/specs/<feature>/spec.md` becomes a
   JUnit row before the code changes, and we see it fail.
2. **Two reviews for each batch, by sessions with no shared context.** The spec review compares the
   diff with `spec.md`, row by row ([`workflow/review-rubric.md`](workflow/review-rubric.md) items
   1–7). The design review checks the code against our style and patterns (item 8). Neither session
   gets the conversation that wrote the code.
3. **Black-box Hurl examples.** A session that has not read the code writes
   `examples/our-<feature>.hurl` from the spec, one request for each example row. We run them against
   a local build first, then against the team URL after the deploy. Example: `our-acceptance.hurl`,
   189/189 on the team URL before the claim.
4. **The security guardian's exploit script.** Each security finding has an exploit. The guardian
   runs all of them against a local build of each batch. Each check prints CLOSED or OPEN, and the
   exit code is the number of OPEN checks. A finding is closed only when its exploit fails.
5. **The merge test.** Before a merge from Carl Kristian's laptop, the architecture guardian merges in a scratch
   worktree and runs the full suite. For Spring Boot 4 (PR #5), Fiffi found 7 fixes that the merge needs.
6. **The request log and the harness transcripts.** `RequestLogFilter` writes one line per request,
   so we can compare our answers with what the checks sent.

**Story (14:13–14:25): our own security fix had a ReDoS, and the guardian caught it before the
push.**

- `9458d53` masked the emails in the request log. The mask ran a regular expression on the whole
  body, and the time grew with the square of the body length.
- Ferdinand, the security guardian, ran the exploit script on the batch before the push. An 80 KB
  body without `@` took 16 s of CPU. A 2 MB body took more than 5 minutes.
- The fix `b7f1b79` cuts the body first, then masks, and bounds the pattern. A 2 MB body now takes
  0.02 s. The new test rows send 80 KB and 2 MB.
- Why it matters: a fix for one finding (emails in the logs, S7) opened a worse hole (S11). Only a
  review that runs the exploit, not one that reads the diff, measured it.
- It also led to two more limits: at most 64 characters before the `@` in an email (`230a439`), and
  at most 200 characters in a talk title (`d8612c3`).

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
| Acceptance | claimed (Nils Christian), after the product owner's «Godkjent» at 14:30:30 CET | 124 | Deployed `c43bbaf`. **Security:** the security guardian's report (`workflow/security-review.md`). **Architecture:** the design review found nothing before the claim, and the architecture guardian's report (`workflow/architecture-review.md`). **Functionality:** rules R1–R9 each have an example table. CI and deploy run 42224526394 green. `our-acceptance.hurl` 189/189 and the base examples passed on the team URL at 13:27:33Z. The 15 base checks were OK at tick 121. Known risk: an unfiltered `GET /talks` took 12–17 s. Details: `workflow/claims/acceptance.md`. After the claim: the 5 acceptance checks were OK at tick 125. |
| Workshop registration | claimed (Nils Christian), after Ingvild Hardeng approved on Stein's behalf at 14:42:57 CET ("Jeg godkjenner for Stein"); Stein himself has not answered in the claim thread | 136 | Deployed `c4fe79c`, then `3318bf1` (same workshop code; R10 proves with the email). **Security:** Ferdinand, S1–S12 closed; S5b (the email is public, so anybody can unregister) is fixed in `b8b5077`, not deployed at the claim. **Architecture:** Frode's design review and Fiffi, no blocker. **Functionality:** R1–R10 each have an example table. Frekken's spec review: no open defect. CI and deploy run 42226345319 green. The deployed `our-workshop-registration.hurl` 211/211 and the base examples passed on the team URL at 13:39:51Z. The 20 claimed checks were OK at tick 133. Details: `workflow/claims/workshop-registration.md`. |
| Scheduling | claimed (Nils Christian), after Stein's own «godkjent» at 15:07:40 CET (top level) and «Godkjent» in the scheduling thread at 15:08:33 CET | 162 | Deployed `7bdf99a`, then `dfd57c9`. **Security:** Ferdinand, no blocker (S10b: anybody can change the programme; the spec has no auth model). **Architecture:** Fiffi, no blocker; 4 "tidy after" items, 2 done. **Functionality:** R1–R8 each have an example table; Stein got a requirement-by-requirement overview. `SchedulingTest` 78/78 (R8 at the same time included), CI and deploy run 42229118539 green. `examples/local/our-scheduling.hurl` 215/215 three times against a local build only, because it changes the shared programme. Read-only check on the team URL: 200 for three places, 400 for `R4/S1`. All 24 claimed checks were OK at tick 161. Details: `workflow/claims/scheduling.md`. |
| Co-speakers | claimed (Nils Christian), after Stein's own «Godkjent» at 15:15:20 CET (top level) and at 15:15:41 CET in Carl Kristian's claim thread | 170 | Co-speakers came from Carl Kristian's team (PR #4, `b729a5e`), and his team prepared the claim request. Deployed `05ac2c5`, which has PR #9 (`dfd57c9`: R9 on `TalkStatus`, sealed result, R11 cascading foreign keys, R12 `actingSpeakerId`). **Security:** our security guardian (Ferdinand) OK'd PR #9 at `3ca19dd` after testing the migration and an attack script (one low finding left). **Architecture:** our architecture guardian (Fiffi) OK'd PR #9; both points (G2, G3) closed. **Functionality:** R1–R12 in `workflow/specs/co-speakers/spec.md`, each with an example table; our spec review found no defect. 545 tests green (Java 21). Deploy run for `dfd57c9` green, and CI and deploy run 42226853638 green. All 537 Hurl requests (9 files) passed on the team URL; earlier, `our-co-speakers.hurl` 66/66 on `3318bf1` at 13:41:50Z. Harness at tick 165: all claimed features OK except `scheduling/check-1`. After the claim: at tick 171 `co-speakers/check-1` (second DELETE of a removed co-speaker, we answer 200) and `check-2` (PUT of the main speaker, we answer 409) were FAULTY; check-3 OK. See section 4, ticks 171–178. Details: `workflow/claims/co-speakers.md`. |
| Ratings change request (`ratings-cr`) | claimed (Nils Christian), after the product owner's «Godkjent» at 15:40:25 CET in the thread of our request | 195 | Deployed `6227fcb` (PR #11, merged as `fff8c83`, deployed together with PR #13). **Security:** Nils Christian's security guardian OK'd PR #11 at `76513a4`: still one query, no division by zero or overflow, edge cases and 1000 ratings tested, attack script all closed but one low finding. **Architecture:** their architecture guardian OK'd it: the rule lives in `RatingTotals.average()`, a pure function; SQL only aggregates. **Functionality:** R1–R6 in `workflow/specs/ratings-cr/spec.md`, each with an example table; tests written first (16 of 29 rows red), 578 tests green (Java 21). `/health` showed `6227fcb`; all 892 Hurl requests (10 files, 354 in `our-ratings-cr.hurl`) passed on the team URL. Harness at tick 190: all claimed features OK. Open (not blocking): whether `/admin/stats` should use the trimmed averages. |
| Submission-cap-cr | claimed (Nils Christian), after Stein's own «Godkjent» at 15:50:15 CET and «Begge er godkjent» in the claim thread | 205 | Deployed `e7304e2`. **Security:** Ferdinand, `f95fd45` OK (no deadlock, R6 exactly 1 success under load). **Architecture:** Fiffi, OK, no blocker (relayed by The Hand): the counting statuses stay in `TalkStatus`, one SQL constant counts for both paths, lock order talk then speaker. **Functionality:** R1–R8 each have an example table; Stein's answer keeps Co-speakers R9. `SubmissionCapTest` R1–R7; `our-submission-cap.hurl` 128/128 and the changed old rows green on a local build of `f95fd45`; Frikke on the candidate: 695 tests, Hurl 1023 + 333 + 225. Details: `workflow/claims/submission-cap-cr.md`. |
| Programme export | claimed (Nils Christian), on the same approval as submission-cap-cr («Begge er godkjent») | 206 | Deployed `e7304e2`. **Security:** Ferdinand, `b700b6e` no blocker (S14, S16 low). **Architecture:** guardian, `b700b6e` no blocker (G22, G23 tidy after). **Functionality:** R1–R7 each have an example table. `examples/local/our-programme-export.hurl` 333/333 against a local build only, because it empties the shared programme. 38/38 table rows. Details: `workflow/claims/programme-export.md`. |

**Workshop-registration claim: an approval that was not the product owner's own.**
We claimed at tick 136 after Ingvild Hardeng wrote "Jeg godkjenner for Stein" in #team-11. Stein
objected at 14:51:33 CET that he gave nobody the authority to approve for him. Nils Christian answered
in Stein's thread at 14:53 that accepting the approval on his behalf was our mistake. The claim is
permanent, so it stays. The rule now: only the product owner's own approval counts, in his own words.

Why we missed it: the claim gate asked for "the product owner's approval" but did not say whose words
count. We took an approval in Stein's name as his. Nobody checked that the person who wrote it was the
product owner.

## 4. What the harness caught that we didn't

<!-- Failures the harness found after you claimed (or after a deploy), and why your own review missed them. -->

**Tick 27 (11:54): `speakers/check-2` and `ratings/check-1` were FAULTY on `POST /speakers`.**
Both were OK again at tick 28. The cause was the deploy of `726c5bc`, not the change in it:

- The new container started at 11:53:28. The first harness tick arrived 30 s later, with about 25
  concurrent `POST /speakers`.
- On the cold JVM they took 3.0–4.2 s each, against about 0.1–0.3 s when warm. All of them got 201,
  so the harness timed out, not our code.
- We found it with the request log (`RequestLogFilter`): the slow lines and the start-up line are
  next to each other.

Why we missed it: our tests, the Hurl files and the deploy gate all run one request at a time. The
deploy gate waits only for `/health` with the new SHA. Nothing measured the first burst of traffic on a
cold JVM. Each deploy, and each start after scale-to-zero, can cost a FAULTY tick.

**Tick 67 (12:34): `speakers/check-1` and `speakers/check-3` were FAULTY on `POST /speakers`.**
The cause was the same: the cold start after the deploy of `0a95814`. Both checks were OK again at
the next tick. The harness alert went to Slack #team-11 from our log and harness watches.

The fix is a warm-up at start-up (`workflow/specs/warm-up/spec.md`, `c03799a`, deploy run
42219441079). After the start, the service sends 6 requests to its own port. The requests change no
data. The first warm-up request now gets the cold cost (2677 ms), not the harness.

| First harness burst after a start | Of the first 35 requests, 1 s or more | Slowest |
|---|---|---|
| Before: containers 00009, 00010, 00011 | 25, 27, 21 | 3.6 s, 3.4 s, 3.9 s |
| After: container 00012 | 0 | 782 ms |

Ticks 68–78 were all OK after the fix, and we lost no more points. We check this (R5) in the request
log after each deploy, because a JUnit test runs on a JVM that is already warm.

**Tick 107: a FAULTY check from a `POST /speakers` that took 2.1 s.** This time the JVM was warm. The
container has a small share of a CPU, and in a burst the JIT compiler took CPU from the requests.

- Fiffi measured the JVM flag `-XX:TieredStopAtLevel=1` (the fast C1 compiler only) on a local
  image at 0.25 CPU: p90 from 690–1080 ms to 480–590 ms, and start-up from 24–29 s to 15–17 s.
- The hotfix `c43bbaf` sets the flag in the `Dockerfile`.
- Filla compared the first 3 minutes after the warm-up in the request log: container 00017
  without the flag, and container 00018 with it (deploy run 42224526394, 13:20 UTC).

| First 3 minutes after the warm-up | Requests | 1 s or more | 2 s or more | Median | p90 | Slowest | Start-up |
|---|---|---|---|---|---|---|---|
| Before: container 00017 | 827 | 205 (25 %) | 39 | 312 ms | 1505 ms | 5197 ms | 34–40 s |
| After: container 00018, `c43bbaf` | 842 | 57 (7 %) | 7 | 198 ms | 818 ms | 3012 ms | 21.4 s |

Why we missed it: no test runs with the container's CPU limit. The warm-up fixed the cold start, but
not a burst on a warm JVM with little CPU.

**Ticks 137–160 (13:44–14:07): `workshop-registration/check-2` was FAULTY on `/workshops/{id}/registrations`, 24 ticks.**
It began at the first tick of the workshop checks after the claim at tick 136. The other three
workshop checks and the 20 earlier checks stayed OK. bled went from −2.63 to −26.63: 24 points.

- Step 1, ticks 137–147 (11 ticks): the check registered for a `SUBMITTED` workshop and got our 409.
  Then it registered for an `ACCEPTED` `LIGHTNING_20` talk and got our 404 "no workshop with this id".
  The check stopped there. Our decision for R2 was 404 for a talk that is not a `WORKSHOP`: "the path
  names a workshop, so the resource does not exist". The fix `a242630` gives 409 for a POST to a talk
  that exists but is not a workshop, as for a workshop that is not accepted. It went live with
  `3393575`.
- Step 2, ticks 148–160 (13 ticks): with the POST fixed, the check went one request further. It read
  the list of the same `LIGHTNING_10` talk with `GET` and got our 404. The fix `dc1a353` gives 200 with
  an empty list. Nobody can register for that talk, so its list is empty, as for a workshop that was
  never accepted. The deploy runs for `dc1a353` and `735e1e3` were cancelled by later pushes, so the
  fix went live only with `dfd57c9` at 14:07:19. Tick 161 was OK.
- An unknown talk ID still gives 404, on POST and on GET.
- We found both steps with the harness timeline (`failure`: first `POST`, then `GET`
  `/workshops/{id}/registrations`) and the transcript of the faulty run. The harness does not show
  the status that it expects.

Why we missed it: we read "only workshops" and "only accepted workshops" as two rules, and gave them
two status codes. The request has one rule: "Only workshops … that the committee has accepted take
registrations". Our review compared the code with our spec, and our spec already had the split, so
the review could not find it. A decision that splits one sentence of the request into two status
codes needs a second look against the request itself.

Why it took 24 ticks:

- A check stops at its first failure, so the transcript at tick 137 showed the POST and nothing after
  it. We fixed one step, and the next step showed only after that deploy. When a check fails on one
  request of a sequence, check the rest of the same flow against the request before the fix is
  deployed. Here that was the list of the same talk.
- The fix waited in a queue with other deploys, and later pushes cancelled two of its deploy runs.
  Deploy a fix for a FAULTY check alone, and let no other push cancel its run.

**Ticks 171–178 (14:18–14:25 UTC): `co-speakers/check-1` and `check-2` were FAULTY, 8 ticks, from the first run after the claim at tick 170.**
The third co-speakers check and the other checks stayed OK.

- `check-1` failed on `DELETE /talks/{id}/co-speakers/{speakerId}` for a speaker who was not, or no
  longer, a co-speaker. We answered 200, the talk unchanged. Our decision was "DELETE is idempotent, like
  R5 for PUT: a double-click on remove must not fail". The harness does not accept it. The fix gives 404.
- `check-2` failed on `PUT /talks/{id}/co-speakers/{speakerId}` with the main speaker as co-speaker. We
  answered 409, "a conflict with the talk's state". The fix gives 400.
- Both fixes are in one commit, `d4ef02a`, made test first by Frekken on our laptop. The guardians
  cleared it, and it was pushed alone. It went live at 14:25:27 UTC, and tick 179 had no co-speakers
  check FAULTY.
- We found both with the harness timeline (`failure`: the `DELETE` and the `PUT`) and the transcripts.

Why we missed it: the request gives no status codes for these two cases, so the spec chose them by
analogy: 200 for a repeated DELETE, as for a repeated PUT, and 409 for a rule about the talk. The
harness reads them as "nothing to remove" (404) and "a request that can never be valid" (400). Our
review compared the code with our spec, so it could not find a status code that the spec itself
chose. It is the same pattern as `workshop-registration/check-2` (ticks 137–160): a status code that
our spec decided by analogy, which the harness reads another way. For a case that the request does
not name, prefer 404 when the thing to change is not there, and 400 when the request can never be
right, and treat 409 and an idempotent 200 as decisions that need a second look.

**Ticks 179 and 184 (14:26 and 14:31 UTC): three checks FAULTY during deploy switches, not from our code.**
Each failure fell in the minutes when a new container took over from the old one.

- Tick 179: `talks/check-4` failed on `POST /talks`, and `ratings/check-5` on `POST /speakers`. Both
  transcripts end with no answer, so the harness timed out. The deploy of `d4ef02a` had started
  container 00026 at 14:25:10, and its warm-up ended at 14:25:14. In the next minute, 126 of 479
  requests took 1 s or more, and the slowest `POST /talks` took 4.6 s. Five minutes later, on the same
  container, the slowest `POST /talks` took 0.7 s.
- Tick 184: `acceptance/check-3` got a 502 on `PUT /talks/{id}/ratings/{reviewerId}`. Container 00027
  started at 14:31:52 and took over from 00026. The request is not in the request log of either
  container, so the platform proxy answered it, not our service. A push of documents only had started
  this deploy.
- The "bled" total in the harness score went from −45.97 at tick 179 to −46.77 at tick 184.
- Fixes: since `81344e6` (14:33 UTC), a push that changes only documents does not deploy. Filla is
  making the warm-up stronger, so that it covers the first minute of traffic, not only the first
  request.

Why we missed it: the warm-up (section 4, tick 67) sends 6 requests, and that was enough for the first
burst after a start. It is not enough for the next minute, while the JIT compiler is still busy. We
also took a deploy as free: about 10 sessions push documents such as this file, and each push
restarted the service while the harness ran.

## 5. Reusable artifacts

<!-- What's worth taking to client work: agents, skills, prompts, spec templates, gates. Give the paths. -->

Draft. Each item works outside this repo with small changes.

| Artifact | Path | What it does |
|---|---|---|
| Harness client | [`workflow/harness.sh`](workflow/harness.sh) | Gets specs, feedback and check transcripts. It never prints the token, and it waits on `429`. |
| Spec template | [`workflow/spec-template.md`](workflow/spec-template.md) | Rules with IDs, our decisions, open questions for the product owner, one example table for each rule. |
| Review rubric | [`workflow/review-rubric.md`](workflow/review-rubric.md) | A review agent checks the diff against the spec, rule by rule and row by row, without the conversation that wrote the code. |
| Claim checklist | [`workflow/claim-checklist.md`](workflow/claim-checklist.md) | A person claims only when each line has evidence that another person can check. |
| Coordination rules | [`workflow/coordination.md`](workflow/coordination.md) | One coordinator session, many agent sessions, one shared working tree. |
| TableTest report | [`src/test/tabletest-templates/status-table.md.peb`](src/test/tabletest-templates/status-table.md.peb), output in [`docs/specs/`](docs/specs/index.md) | Turns the `@TableTest` example tables into Markdown pages for the product owners, with ✅ or ❌ on each row. |
| Security guardian | [`workflow/security-review.md`](workflow/security-review.md) | A session that writes no fixes reviews each batch against 8 checks (SQL, authorisation, error answers, input size, personal data, limits under concurrency, exports, dependencies). Each finding has a decision, a fix and an exploit re-run. |
| Exploit script | [`workflow/security/exploits.sh`](workflow/security/exploits.sh) | One exploit for each finding (S1–S12). It prints CLOSED or OPEN for each, and its exit code is the number of OPEN checks. A finding is closed only when its exploit fails. The script writes test data, so run it against a local build: `workflow/security/exploits.sh http://localhost:8080`. |
| Architecture guardian | [`workflow/architecture-review.md`](workflow/architecture-review.md) | Our patterns P1–P8 with an example of each, and a log of each batch review with the named refactoring and its commit. |
| Claim evidence | [`workflow/claims/`](workflow/claims/) | One file for each feature, in three sections (security, architecture, functionality). The product owner approves from it. |
| Start-up warm-up | [`StartupWarmUp.java`](src/main/java/no/scienta/cfp/StartupWarmUp.java), spec in [`workflow/specs/warm-up/spec.md`](workflow/specs/warm-up/spec.md) | The service sends requests to its own port after a start, so the first real traffic does not get the cold-JVM cost (section 4). |

## 6. One thing we'd do differently

<!-- One thing, and why. -->

(draft, the team decides) Measure a burst of traffic before the first deploy, on a cold start and with the container's CPU limit. We learnt both from FAULTY ticks (27, 67 and 107).
