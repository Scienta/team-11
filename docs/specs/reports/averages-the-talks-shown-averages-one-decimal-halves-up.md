## Averages the talks' shown averages, one decimal, halves up

One speaker submits a talk for each list of scores; each score comes from a different
reviewer. A talk nobody rated does not count.


| Result | Scenario | Scores of each talk | Average Rating? |
| --- | --- | --- | --- |
| ✅ passes | No ratings | [[]] |  |
| ✅ passes | One talk | [[4, 5]] | 4.5 |
| ✅ passes | Averages 4.0 and 4.5 | [[4], [4, 5]] | 4.3 |
| ✅ passes | An unrated talk is left out | [[4], []] | 4.0 |
