## R9: A withdrawn or rejected talk can't be changed

The talk has Ada as co-speaker. Main then withdraws it with POST /talks/{id}/withdraw, or
the committee decides with PUT /talks/{id}/status, after two ratings for ACCEPTED.
PUT adds Bob, DELETE removes Ada. A blank message is not checked.


| Result | Scenario | Talk status | Request | Status? | Message? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | Out for good | {WITHDRAWN, REJECTED} | {PUT Bob, DELETE Ada} | 409 | a withdrawn or rejected talk can't be changed |
| ✅ passes | Still on offer | {SUBMITTED, WAITLISTED, ACCEPTED} | {PUT Bob, DELETE Ada} | 200 |  |
