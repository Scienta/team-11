## R6: Ranks talks by shown average, then rating count, then age

Talks A, B and C are submitted in this order, so A is the oldest. Each score comes from a
different reviewer. An empty list means that nobody has rated the talk. The ranking is
GET /talks?sort=rating.


| Result | Scenario | Talk A | Talk B | Talk C | Ranking? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | Different averages | [3] | [5] | [4] | [B, C, A] |
| ✅ passes | Same average, different counts | [4] | [4, 4] | [4, 4, 4] | [C, B, A] |
| ✅ passes | Averages differ at 2 decimals | [4, 4, 4, 5] | [4, 4, 5] | [5] | [C, B, A] |
| ✅ passes | Same average and count | [4] | [4] | [4] | [A, B, C] |
| ✅ passes | Unrated talks | [] | [1] | [] | [B, A, C] |
