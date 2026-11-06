## R5: Lists all talks oldest first, or the talks of one speaker

Speaker 1 submits talk A, speaker 2 submits talk B, then speaker 1 submits talk C.
A blank speaker sends GET /talks without a speakerId.


| Scenario | Speaker | Talks? |
| --- | --- | --- |
| All speakers |  | [A, B, C] |
| One speaker | 1 | [A, C] |
| Another speaker | 2 | [B] |
| Speaker that is unknown | 3 | [] |
