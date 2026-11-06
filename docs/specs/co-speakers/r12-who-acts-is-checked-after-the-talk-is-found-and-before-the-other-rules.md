## R12: Who acts is checked after the talk is found and before the other rules

The talk has Ada as co-speaker and the talk status given, or is a talk id that does not
exist. Acting is the actingSpeakerId query parameter.


| Result | Scenario | Talk | Request | Acting | Status? | Message? |
| --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | Unknown talk first | unknown | {PUT Bob, DELETE Ada} | Cy | 404 | talk not found |
| ✅ passes | Before an unregistered speaker | SUBMITTED | {PUT Nobody, DELETE Nobody} | Cy | 403 | only the main speaker can change the co-speakers |
| ✅ passes | Before the main speaker as target | SUBMITTED | {PUT Main, DELETE Main} | Cy | 403 | only the main speaker can change the co-speakers |
| ✅ passes | Before the status | {WITHDRAWN, REJECTED} | {PUT Bob, DELETE Ada} | Cy | 403 | only the main speaker can change the co-speakers |
| ✅ passes | The main speaker gets the status | {WITHDRAWN, REJECTED} | {PUT Bob, DELETE Ada} | Main | 409 | a withdrawn or rejected talk can't be changed |
