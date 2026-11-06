# Coordination: many agent sessions on one laptop

One laptop runs many Claude sessions in one working tree. These rules keep the sessions out of each
other's way, and give the person one place to look.

## One coordinator: The Hand

- One coordinator session, "The Hand", takes all requests: from the person, from the agent
  sessions and from Slack.
- The Hand changes no files and runs no investigations. It gives each task to an agent session.
- The person follows only The Hand's session.

## Agent sessions talk to The Hand only

- An agent session sends every question, status and approval request to The Hand with
  `SendMessage`. It never asks the person directly.
- Only The Hand posts in Slack. A person takes product owner questions to the product owner.

## Claim the task in Slack first

- Before The Hand gives a task to an agent, The Hand claims the task in Slack `#team-11`.
- Reason: another laptop on the team (Carl Kristian's) runs agents too. The claim stops two
  laptops from doing the same work.

## Watchers

- One session watches Slack (read only) and reports new messages to The Hand.
- One session watches the deploy, the request log and the harness, and posts alerts.

## Shared working tree

- Stage by path: `git add <path>`. Never `git add -A`, `git add .` or `git commit -a`. All
  sessions share one git index.
- Commit with explicit paths: `git add <path> && git commit -m "…" -- <path>`. Then the commit
  holds only your own files, whatever other sessions have staged. Never leave changes staged.
  Reason: at 13:45 one session ran `rm -rf docs/specs` and staged the deletion. The next commit of
  another session took the deletion from the shared index (`6841a88`, repaired in `5eff9ac`).
- Never stage a partial hunk through the shared index (`git add -p`, `git apply --cached`).
  Another session can commit the index at the same moment. When another session has uncommitted
  changes in a file that you need, wait until it commits.
- Never `rm -rf` a shared path in the main working tree. It can delete the uncommitted work of
  another session.
- Run tests in your own `git worktree` of `HEAD`, with your changed files copied in. Reason:
  another session's Maven run can rebuild the shared `target/` during yours (Frikke got a
  `ClassNotFoundException`).
- Before you commit from the main tree, check that each file is byte-equal to the file you
  tested: `cmp <path> <worktree>/<path>`.
- One owner at a time for a shared file, for example `docs/specs/` and `REVIEW.md`. The Hand
  says who the owner is.
- Make reports, such as the TableTest report (`docs/specs/`), in a separate `git worktree` of
  `HEAD`. Then the report does not show uncommitted work from other sessions.
- Pushes go through The Hand. A push deploys. A person approves each push, or The Hand approves it on
  the person's behalf when all of these hold:
  - the push candidate is built on the current `origin/main`;
  - the full suite is green;
  - the security and architecture guardians report no blocker;
  - the pusher pushes by SHA and stops if `origin/main` moved.

  The Hand reports each such push to the person. (The user's decision, 14:40.)
- One push can carry the commits of several sessions.

## Independent checks

- A session that does not read the code writes the Hurl examples (`examples/our-<feature>.hurl`)
  from the spec.
- A session that has not seen the code reviews the diff against the spec, with
  [`review-rubric.md`](review-rubric.md).
- Reason: a check from the same context as the code repeats the mistakes of the code.
