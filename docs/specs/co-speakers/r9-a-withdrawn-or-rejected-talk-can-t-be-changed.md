## R9: A withdrawn or rejected talk can't be changed

The talk has Ada as co-speaker; its status is then set in the database, because the
acceptance feature that changes it is not built yet. PUT adds Bob, DELETE removes Ada.


| Scenario | Talk status | Request | Status? |
| --- | --- | --- | --- |
| Out for good | {WITHDRAWN, REJECTED} | {PUT Bob, DELETE Ada} | 409 |
| Still on offer | {SUBMITTED, WAITLISTED, ACCEPTED} | {PUT Bob, DELETE Ada} | 200 |
