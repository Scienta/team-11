## R5, R6: Writes a stored title into the CSV as the print shop needs it

Talk A is talk 1, a presentation by Grace Hopper, in R1 S1. The CSV is read as UTF-8 bytes.
In the cells, ⏎ is a newline.


| Result | Scenario | Title | CSV Body? |
| --- | --- | --- | --- |
| ✅ passes | Letters outside ASCII | Kåre og Åse 🚀 | slot,start,end,room,talkId,title,format,speakers⏎S1,09:00,10:00,R1,1,Kåre og Åse 🚀,PRESENTATION,Grace Hopper⏎ |
| ✅ passes | Comma and quotes | Kotlin, "really" fast | slot,start,end,room,talkId,title,format,speakers⏎S1,09:00,10:00,R1,1,"Kotlin, ""really"" fast",PRESENTATION,Grace Hopper⏎ |
| ✅ passes | Line break in the title | Two⏎lines | slot,start,end,room,talkId,title,format,speakers⏎S1,09:00,10:00,R1,1,"Two⏎lines",PRESENTATION,Grace Hopper⏎ |
