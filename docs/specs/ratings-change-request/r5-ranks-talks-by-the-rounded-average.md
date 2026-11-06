## R5: Ranks talks by the rounded average

Talks A, B, C and D are submitted in this order, so A is the oldest. Each score comes from a
different reviewer; 3x7 means 7 reviewers who each gave 3. An empty list means that nobody has
rated the talk. Ada submits A and B, Grace submits C and D: a speaker submits at most 3
talks. The talks in Withdrawn are withdrawn after the ratings. The ranking is
GET /talks?sort=rating.


| Result | Scenario | Talk A | Talk B | Talk C | Talk D | Withdrawn | Ranking? |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | Same rounded average, more ratings first | [5, 4, 3x7, 1] | [5, 4x2, 3x15, 1] | [] | [] | [] | [B, A, C, D] |
| ✅ passes | Same average and count, oldest first | [4, 5] | [4, 5] | [] | [] | [] | [A, B, C, D] |
| ✅ passes | The trimmed averages | [5, 5, 5, 1, 1] | [1, 5, 5, 5] | [5, 5, 5, 5, 1] | [] | [] | [C, B, A, D] |
| ✅ passes | Two decimals tell close talks apart | [] | [4, 4, 4, 5] | [4, 4, 5] | [] | [] | [C, B, A, D] |
| ✅ passes | Unrated last, oldest first | [] | [1] | [] | [] | [] | [B, A, C, D] |
| ✅ passes | Withdrawn after all others | [4, 5] | [3] | [] | [] | [A] | [B, C, D, A] |
