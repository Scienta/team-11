## Ranks talks only for the sort order rating

A blank sort sends GET /talks without a sort, which lists the talks oldest first.


| Result | Scenario | Sort | Status? |
| --- | --- | --- | --- |
| ✅ passes | No sort |  | 200 |
| ✅ passes | Rating | rating | 200 |
| ✅ passes | Another sort order | {date, title} | 400 |
| ✅ passes | Rating in capitals | RATING | 400 |
