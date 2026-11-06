## R5: Lists all talks oldest first, or the talks of one speaker

Speaker 1 submits talk A, speaker 2 submits talk B, then speaker 1 submits talk C.
A blank speaker sends GET /talks without a speakerId.


| Result | Scenario | Speaker | Talks? |
| --- | --- | --- | --- |
| ✅ passes | All speakers |  | [A, B, C] |
| ✅ passes | One speaker | 1 | [A, C] |
| ✅ passes | Another speaker | 2 | [B] |
| ✅ passes | Speaker that is unknown | 3 | [] |
