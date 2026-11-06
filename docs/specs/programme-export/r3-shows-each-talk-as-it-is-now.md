## R3: Shows each talk as it is now

Talk A is talk 1, by Grace Hopper, and the change happens after it is on the programme. The
JSON row and the CSV line are compared as text.


| Result | Scenario | Title | Format | Co-speakers | Place | Change | JSON Row? | CSV Line? |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | A presentation | Legacy is a feeling | PRESENTATION | [] | R1/S1 |  | {"slot":"S1","start":"09:00","end":"10:00","room":"R1","talkId":1,"title":"Legacy is a feeling","format":"PRESENTATION","speakers":["Grace Hopper"]} | S1,09:00,10:00,R1,1,Legacy is a feeling,PRESENTATION,Grace Hopper |
| ✅ passes | A workshop | Hands on | WORKSHOP | [] | R2/S4 |  | {"slot":"S4","start":"13:30","end":"15:30","room":"R2","talkId":1,"title":"Hands on","format":"WORKSHOP","speakers":["Grace Hopper"]} | S4,13:30,15:30,R2,1,Hands on,WORKSHOP,Grace Hopper |
| ✅ passes | Withdrawn afterwards | Legacy is a feeling | PRESENTATION | [] | R3/S5 | withdrawn | {"slot":"S5","start":"15:45","end":"16:45","room":"R3","talkId":1,"title":"Legacy is a feeling","format":"PRESENTATION","speakers":["Grace Hopper"]} | S5,15:45,16:45,R3,1,Legacy is a feeling,PRESENTATION,Grace Hopper |
| ✅ passes | Co-speaker added afterwards | Legacy is a feeling | PRESENTATION | [] | R1/S2 | Ada Lovelace added | {"slot":"S2","start":"10:15","end":"11:15","room":"R1","talkId":1,"title":"Legacy is a feeling","format":"PRESENTATION","speakers":["Grace Hopper","Ada Lovelace"]} | S2,10:15,11:15,R1,1,Legacy is a feeling,PRESENTATION,Grace Hopper; Ada Lovelace |
| ✅ passes | Co-speaker removed afterwards | Legacy is a feeling | PRESENTATION | [Ada Lovelace] | R1/S3 | Ada Lovelace removed | {"slot":"S3","start":"11:30","end":"12:30","room":"R1","talkId":1,"title":"Legacy is a feeling","format":"PRESENTATION","speakers":["Grace Hopper"]} | S3,11:30,12:30,R1,1,Legacy is a feeling,PRESENTATION,Grace Hopper |
