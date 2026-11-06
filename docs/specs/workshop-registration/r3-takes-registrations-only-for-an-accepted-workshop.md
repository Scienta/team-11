## R3: Takes registrations only for an accepted workshop

Each row brings a new workshop to the status through the acceptance API: two ratings
before ACCEPTED, and an accepted workshop withdrawn by its speaker for WITHDRAWN.


| Result | Scenario | Workshop Status | Status? |
| --- | --- | --- | --- |
| ✅ passes | Accepted | ACCEPTED | 201 |
| ✅ passes | Not accepted | {SUBMITTED, WAITLISTED, REJECTED, WITHDRAWN} | 409 |
