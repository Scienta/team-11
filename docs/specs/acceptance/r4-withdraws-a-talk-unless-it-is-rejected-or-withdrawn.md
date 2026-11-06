## R4: Withdraws a talk unless it is rejected or withdrawn

The main speaker asks. The talk has ratings from two different reviewers before it gets its
status before. A refused withdrawal leaves the status as it was (R6).


| Result | Scenario | Status Before | Response? | Status After? |
| --- | --- | --- | --- | --- |
| ✅ passes | Talk in play | {SUBMITTED, WAITLISTED, ACCEPTED} | 200 | WITHDRAWN |
| ✅ passes | Rejected talk | REJECTED | 409 | REJECTED |
| ✅ passes | Withdrawn talk | WITHDRAWN | 409 | WITHDRAWN |
