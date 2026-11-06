## R1, R2: Counts a reviewer who rates again once, with the latest score

The ratings are sent in the order of the list.


| Result | Scenario | Ratings | Average Rating? | Rating Count? |
| --- | --- | --- | --- | --- |
| ✅ passes | Four reviewers stay below the trim | [[reviewer: 1, score: 1], [reviewer: 1, score: 5], [reviewer: 2, score: 5], [reviewer: 3, score: 5], [reviewer: 4, score: 1]] | 4.0 | 4 |
| ✅ passes | Five reviewers, the latest score trimmed | [[reviewer: 1, score: 5], [reviewer: 1, score: 1], [reviewer: 2, score: 1], [reviewer: 3, score: 3], [reviewer: 4, score: 4], [reviewer: 5, score: 5]] | 2.67 | 5 |
| ✅ passes | One reviewer rates again | [[reviewer: 1, score: 2], [reviewer: 1, score: 5]] | 5.0 | 1 |
