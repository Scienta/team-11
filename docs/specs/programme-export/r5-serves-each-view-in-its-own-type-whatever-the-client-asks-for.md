## R5: Serves each view in its own type, whatever the client asks for

The programme is empty. A blank Accept sends no Accept header. The body is read as UTF-8
bytes, so a byte order mark in front of the header would show.


| Result | Scenario | Path | Accept | Status? | Content Type? | Body? |
| --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | CSV | /programme.csv | {"", text/csv, */*} | 200 | text/csv;charset=UTF-8 | slot,start,end,room,talkId,title,format,speakers⏎ |
| ✅ passes | CSV for a JSON client | /programme.csv | application/json | 200 | text/csv;charset=UTF-8 | slot,start,end,room,talkId,title,format,speakers⏎ |
| ✅ passes | JSON | /programme | {"", application/json, */*} | 200 | application/json;charset=UTF-8 | [] |
| ✅ passes | JSON for a CSV client | /programme | text/csv | 200 | application/json;charset=UTF-8 | [] |
