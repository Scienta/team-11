## R1: Takes only the three decisions as a status

| Result | Scenario | Body | Response? |
| --- | --- | --- | --- |
| ✅ passes | A decision | {"status":"WAITLISTED"} | 200 |
| ✅ passes | Not a decision | {{"status":"SUBMITTED"}, {"status":"WITHDRAWN"}} | 400 |
| ✅ passes | Not a status | {{"status":"accepted"}, {"status":"MAYBE"}, {"status":""}, {"status":1}} | 400 |
| ✅ passes | No status | {} | 400 |
