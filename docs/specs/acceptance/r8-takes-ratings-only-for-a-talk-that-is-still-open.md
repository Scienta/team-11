## R8: Takes ratings only for a talk that is still open

Reviewers 1 and 2 rate the talk before it gets its status. Then reviewer 3 rates it.


| Result | Scenario | Status | Response? | Rating Count After? |
| --- | --- | --- | --- | --- |
| ✅ passes | Open talk | {SUBMITTED, WAITLISTED} | 200 | 3 |
| ✅ passes | Decided talk | {ACCEPTED, REJECTED, WITHDRAWN} | 409 | 2 |
