## R3: Accepts a talk only after two different reviewers

Each number is one rating by that reviewer, in this order. Reviewer 1 twice is one reviewer
whose second score replaces the first.


| Result | Scenario | Ratings From Reviewers | Min Reviewers | Decision | Response? | Status After? |
| --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | No ratings | [] | 2 | ACCEPTED | 409 | SUBMITTED |
| ✅ passes | One reviewer | [1] | 2 | ACCEPTED | 409 | SUBMITTED |
| ✅ passes | One reviewer who rated twice | [1, 1] | 2 | ACCEPTED | 409 | SUBMITTED |
| ✅ passes | Two reviewers | [1, 2] | 2 | ACCEPTED | 200 | ACCEPTED |
| ✅ passes | No ratings, rejection | [] | 2 | REJECTED | 200 | REJECTED |
| ✅ passes | No ratings, waitlist | [] | 2 | WAITLISTED | 200 | WAITLISTED |
