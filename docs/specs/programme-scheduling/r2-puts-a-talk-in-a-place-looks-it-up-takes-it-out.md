## R2: Puts a talk in a place, looks it up, takes it out

A and B are accepted presentations of two different speakers. The programme before is set up
with PUT. The requests of a row are sent one after the other, and the entry is the one in the
answer to the last request. The programme after is read with GET on all 15 places.


| Result | Scenario | Programme Before | Requests | Statuses? | Entry? | Programme After? |
| --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | Empty place | [:] | [PUT A R1/S1] | [200] | [R1/S1: A] | [R1/S1: A] |
| ✅ passes | Filled place looked up | [R1/S1: A] | [GET R1/S1] | [200] | [R1/S1: A] | [R1/S1: A] |
| ✅ passes | Empty place looked up | [R1/S1: A] | [GET R1/S2] | [404] |  | [R1/S1: A] |
| ✅ passes | Filled place emptied | [R1/S1: A] | [DELETE R1/S1, GET R1/S1] | [204, 404] |  | [:] |
| ✅ passes | Empty place emptied | [:] | [DELETE R1/S1] | [404] |  | [:] |
| ✅ passes | Same place emptied twice | [R1/S1: A] | [DELETE R1/S1, DELETE R1/S1] | [204, 404] |  | [:] |
| ✅ passes | Taken out, put somewhere else | [R1/S1: A] | [DELETE R1/S1, PUT A R2/S3] | [204, 200] | [R2/S3: A] | [R2/S3: A] |
| ✅ passes | Same talk in same place again | [R1/S1: A] | [PUT A R1/S1] | [200] | [R1/S1: A] | [R1/S1: A] |
