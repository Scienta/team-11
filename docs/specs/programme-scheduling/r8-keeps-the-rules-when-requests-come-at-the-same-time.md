## R8: Keeps the rules when requests come at the same time

All requests of a row are sent at the same time to an empty programme. Every talk is an
accepted presentation; a talk the speakers do not name has a speaker of its own. The service
uses at most 5 database connections.


| Result | Scenario | Speakers | Requests At Once | Succeeded? | Refused? | Entries After? |
| --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | Ten talks for one place | [:] | [PUT A R1/S1, PUT B R1/S1, PUT C R1/S1, PUT D R1/S1, PUT E R1/S1, PUT F R1/S1, PUT G R1/S1, PUT H R1/S1, PUT I R1/S1, PUT J R1/S1] | 1 | 9 | 1 |
| ✅ passes | One talk for ten places | [:] | [PUT A R1/S1, PUT A R2/S1, PUT A R3/S1, PUT A R1/S2, PUT A R2/S2, PUT A R3/S2, PUT A R1/S3, PUT A R2/S3, PUT A R3/S3, PUT A R1/S5] | 1 | 9 | 1 |
| ✅ passes | One speaker, two rooms, one slot | [A: Ada, B: Ada] | [PUT A R1/S2, PUT B R2/S2] | 1 | 1 | 1 |
