## R2: Moves a talk to the committee's decision

The talk has ratings from two different reviewers before it gets its status before.
A refused decision leaves the status as it was (R6).


| Result | Scenario | Status Before | Decision | Response? | Status After? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | Open talk accepted | {SUBMITTED, WAITLISTED} | ACCEPTED | 200 | ACCEPTED |
| ✅ passes | Open talk rejected | {SUBMITTED, WAITLISTED} | REJECTED | 200 | REJECTED |
| ✅ passes | Submitted talk waitlisted | SUBMITTED | WAITLISTED | 200 | WAITLISTED |
| ✅ passes | Same decision again | WAITLISTED | WAITLISTED | 409 | WAITLISTED |
| ✅ passes | Accepted talk | ACCEPTED | {ACCEPTED, REJECTED, WAITLISTED} | 409 | ACCEPTED |
| ✅ passes | Rejected talk | REJECTED | {ACCEPTED, REJECTED, WAITLISTED} | 409 | REJECTED |
| ✅ passes | Withdrawn talk | WITHDRAWN | {ACCEPTED, REJECTED, WAITLISTED} | 409 | WITHDRAWN |
