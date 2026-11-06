## R3: Keeps one rating per reviewer, the latest score

The ratings are sent in the order of the list. The average and the count are read from
GET /talks/{id}.


| Scenario | Ratings | Average Rating? | Rating Count? |
| --- | --- | --- | --- |
| One reviewer rates again | [[reviewer: 1, score: 2], [reviewer: 1, score: 5]] | 5.0 | 1 |
| Two reviewers rate once | [[reviewer: 1, score: 2], [reviewer: 2, score: 5]] | 3.5 | 2 |
