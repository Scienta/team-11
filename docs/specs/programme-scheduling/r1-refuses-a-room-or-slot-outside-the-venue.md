## R1: Refuses a room or slot outside the venue

A PUT sends the ID of an accepted presentation. Rooms and slots are written in capitals. A
place that is not in the venue does not exist, like an empty place (harness tick 163).


| Result | Scenario | Method | Place | Status? |
| --- | --- | --- | --- | --- |
| ✅ passes | Room not in the list | {PUT, GET, DELETE} | {R0/S2, R4/S1} | 404 |
| ✅ passes | Slot not in the list | {PUT, GET, DELETE} | R1/S6 | 404 |
| ✅ passes | Room in lower case | {PUT, GET, DELETE} | r1/S1 | 404 |
| ✅ passes | Slot in lower case | {PUT, GET, DELETE} | R1/s1 | 404 |
