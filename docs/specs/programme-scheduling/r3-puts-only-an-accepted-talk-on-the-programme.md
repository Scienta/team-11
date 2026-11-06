## R3: Puts only an accepted talk on the programme

Each row brings presentation A to the status through the acceptance API, then puts A in
R1/S1. A withdrawn talk was accepted before its speaker withdrew it.


| Result | Scenario | Talk Status | Status? | Saved? |
| --- | --- | --- | --- | --- |
| ✅ passes | Accepted | ACCEPTED | 200 | true |
| ✅ passes | Not accepted | {SUBMITTED, WAITLISTED, REJECTED, WITHDRAWN} | 409 | false |
