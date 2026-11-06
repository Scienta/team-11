## R4: Lets only the main speaker withdraw a talk

Speaker 1 submits the talk. Speaker 2 is registered too. Speaker 99 does not exist.
A blank speaker sends a body without speakerId.


| Result | Scenario | Speaker Asking | Response? | Status After? |
| --- | --- | --- | --- | --- |
| ✅ passes | The main speaker | 1 | 200 | WITHDRAWN |
| ✅ passes | Somebody else | {2, 99} | 403 | SUBMITTED |
| ✅ passes | No speaker |  | 400 | SUBMITTED |
