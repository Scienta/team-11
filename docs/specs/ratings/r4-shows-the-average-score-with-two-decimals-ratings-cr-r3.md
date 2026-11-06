## R4: Shows the average score with two decimals (ratings-cr R3)

Each score comes from a different reviewer. The average and the count are read from
GET /talks/{id}; the other talk responses carry the same two fields.


| Result | Scenario | Scores | Average Rating? | Rating Count? |
| --- | --- | --- | --- | --- |
| ✅ passes | No ratings | [] |  | 0 |
| ✅ passes | Average with one decimal | [4, 5] | 4.5 | 2 |
| ✅ passes | Average with two decimals | [4, 4, 4, 5] | 4.25 | 4 |
