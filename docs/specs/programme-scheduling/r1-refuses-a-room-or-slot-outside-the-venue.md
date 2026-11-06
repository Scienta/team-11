## R1: Refuses a room or slot outside the venue

A PUT sends the ID of an accepted presentation. Rooms and slots are written in capitals.


| Result | Scenario | Method | Place | Status? |
| --- | --- | --- | --- | --- |
| ✅ passes | Room R4 | {PUT, GET, DELETE} | R4/S1 | 400 |
| ✅ passes | Slot S6 | {PUT, GET, DELETE} | R1/S6 | 400 |
| ✅ passes | Room in lower case | {PUT, GET, DELETE} | r1/S1 | 400 |
| ✅ passes | Slot in lower case | {PUT, GET, DELETE} | R1/s1 | 400 |
