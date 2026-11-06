## Ranks talks only for the sort order rating

A blank sort sends GET /talks without a sort, which lists the talks oldest first.


| Scenario | Sort | Status? |
| --- | --- | --- |
| No sort |  | 200 |
| Rating | rating | 200 |
| Another sort order | {date, title} | 400 |
| Rating in capitals | RATING | 400 |
