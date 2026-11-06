# Review rubric

The review agent gets the spec (`workflow/specs/<feature>/spec.md`) and the diff. It does not get
the conversation that produced the code. It reports each defect with the rule ID and the file and
line.

1. **Rule coverage.** For each rule ID: name the code that implements the rule and the test that
   proves it. A rule with no test is a defect.
2. **Example rows.** For each row in the example tables: does the code give that status and body?
   Trace it through the code. Do not guess.
3. **Our decisions.** Does each status code in "Our decisions" match the code?
4. **Concurrency.** Can two requests at the same time break a limit or a uniqueness rule? Use row
   locks (`SELECT … FOR UPDATE`) or conditional writes. Advisory locks are not available.
5. **Existing behaviour.** Does the diff change a response that a base feature or an earlier drip
   feature depends on?
6. **Platform.** No session `SET` outside a transaction. No more than 5 connections. No files on
   disk that must survive a deploy.
7. **Input.** SQL with string concatenation of user input is a defect. Unknown or extra JSON fields
   must not cause a 500.
8. **Design.** A second review, after items 1–7. A different session does it, and it loads the
   `coding:java-kotlin-style` and `coding:refactor` skills first. It checks the code in the diff
   against our style:
   - Command/query separation: a query has no side effect, and a command returns no domain data.
   - Pure functions and immutable data at the core. Side effects (database, HTTP, clock, log) at
     the edges.
   - Names with a business meaning, not the mechanism.
   - A `Result` or a sum type for an expected outcome. An exception only for a panic.
   - One level of abstraction in each method.
   - Records for data. No `var`. Each class has a JavaDoc that states its responsibility. Streams,
     not loops.

   Each finding names the refactoring that fixes it, for example *Extract Method* or *Replace
   Conditional with Polymorphism*. Each finding also says when to fix it:
   - **before the claim**: the finding can give a wrong answer, or it stops a test from checking a
     rule.
   - **tidy after**: all other findings. Fix them later, in a separate `refactor:` commit.
