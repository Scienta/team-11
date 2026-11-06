## R5, R6, R7: Writes the header, then one line per row

Each place gets a talk of its own: the first place talk 1, the next talk 2. Each talk is a
PRESENTATION by Grace Hopper with the title Talk and its ID.


| Result | Scenario | Places | Body? |
| --- | --- | --- | --- |
| ✅ passes | Empty programme | [] | slot,start,end,room,talkId,title,format,speakers⏎ |
| ✅ passes | One talk | [R1/S1] | slot,start,end,room,talkId,title,format,speakers⏎S1,09:00,10:00,R1,1,Talk 1,PRESENTATION,Grace Hopper⏎ |
| ✅ passes | Two talks | [R1/S1, R2/S4] | slot,start,end,room,talkId,title,format,speakers⏎S1,09:00,10:00,R1,1,Talk 1,PRESENTATION,Grace Hopper⏎S4,13:30,15:30,R2,2,Talk 2,PRESENTATION,Grace Hopper⏎ |
