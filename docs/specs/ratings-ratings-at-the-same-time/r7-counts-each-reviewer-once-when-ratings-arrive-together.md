## R7: Counts each reviewer once when ratings arrive together

All requests of a row are sent at the same time to one newly submitted talk, each with
score 4. Reviewers are numbered from 1. A copy is the same rating sent again, as from a
double-click. The service uses at most 5 database connections.


| Result | Scenario | Reviewers | Copies Each | Answers? | Rating Count? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | Many reviewers at once | 20 | 1 | {200} | 20 |
| ✅ passes | One reviewer double-clicks | 1 | 2 | {200} | 1 |
| ✅ passes | Many reviewers double-click | 10 | 2 | {200} | 10 |
