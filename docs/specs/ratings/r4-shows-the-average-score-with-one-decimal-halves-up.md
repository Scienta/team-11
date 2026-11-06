## R4: Shows the average score with one decimal, halves up

Each score comes from a different reviewer. The average and the count are read from
GET /talks/{id}; the other talk responses carry the same two fields.


| Result | Scenario | Scores | Average Rating? | Rating Count? |
| --- | --- | --- | --- | --- |
| ✅ passes | No ratings | [] |  | 0 |
| ✅ passes | Average with one decimal | [4, 5] | 4.5 | 2 |
| ✅ passes | Average with a second half | [4, 4, 4, 5] | 4.3 | 4 |
