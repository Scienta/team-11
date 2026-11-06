## R1, R2, R4, R6: Leaves out one highest and one lowest score from 5 ratings

Each score comes from a different reviewer; 4x39 means 39 reviewers who each gave 4.
The average is the JSON text of averageRating in GET /talks/{id}; a blank average is JSON null.


| Result | Scenario | Scores | Average Rating? | Rating Count? |
| --- | --- | --- | --- | --- |
| ✅ passes | No ratings | [] |  | 0 |
| ✅ passes | R1: one rating | [5] | 5.0 | 1 |
| ✅ passes | R1: two ratings | [4, 5] | 4.5 | 2 |
| ✅ passes | R1: a third rounds down | [1, 1, 2] | 1.33 | 3 |
| ✅ passes | R1: two thirds round up | [1, 2, 2] | 1.67 | 3 |
| ✅ passes | R1: a quarter keeps two decimals | [4, 4, 4, 5] | 4.25 | 4 |
| ✅ passes | R1: no trim at 4 ratings | [1, 5, 5, 5] | 4.0 | 4 |
| ✅ passes | R2: one of each end left out | [1, 2, 3, 4, 5] | 3.0 | 5 |
| ✅ passes | R2: only one of the tied top and bottom | [5, 5, 5, 1, 1] | 3.67 | 5 |
| ✅ passes | R2: the lowest left out at 5 ratings | [1, 5, 5, 5, 5] | 5.0 | 5 |
| ✅ passes | R2: all the same | [5, 5, 5, 5, 5] | 5.0 | 5 |
| ✅ passes | R2: tied bottom | [1, 1, 1, 1, 5] | 1.0 | 5 |
| ✅ passes | R2: six ratings | [1, 2, 2, 3, 4, 5] | 2.75 | 6 |
| ✅ passes | R4: every reviewer counts | [5x2, 4x39, 1] | 4.02 | 42 |
