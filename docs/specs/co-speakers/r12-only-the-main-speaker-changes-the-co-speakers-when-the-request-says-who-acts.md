## R12: Only the main speaker changes the co-speakers, when the request says who acts

The talk has Ada as co-speaker. PUT adds Bob, DELETE removes Ada. Acting is the
actingSpeakerId query parameter: a blank cell sends none, '' sends an empty value.
A blank message is not checked.


| Result | Scenario | Request | Acting | Status? | Message? | Co-speakers after? |
| --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | No acting speaker, as before | PUT Bob |  | 200 |  | [Ada, Bob] |
| ✅ passes | No acting speaker, as before | DELETE Ada |  | 200 |  | [] |
| ✅ passes | An empty value, as none | PUT Bob | "" | 200 |  | [Ada, Bob] |
| ✅ passes | An empty value, as none | DELETE Ada | "" | 200 |  | [] |
| ✅ passes | The main speaker | PUT Bob | Main | 200 |  | [Ada, Bob] |
| ✅ passes | The main speaker | DELETE Ada | Main | 200 |  | [] |
| ✅ passes | A co-speaker | {PUT Bob, DELETE Ada} | Ada | 403 | only the main speaker can change the co-speakers | [Ada] |
| ✅ passes | Another registered speaker | {PUT Bob, DELETE Ada} | Cy | 403 | only the main speaker can change the co-speakers | [Ada] |
| ✅ passes | An unregistered speaker | {PUT Bob, DELETE Ada} | Nobody | 403 | only the main speaker can change the co-speakers | [Ada] |
| ✅ passes | No speaker has id 0 or below | {PUT Bob, DELETE Ada} | {0, -1} | 403 | only the main speaker can change the co-speakers | [Ada] |
| ✅ passes | Acting id that is not a number | {PUT Bob, DELETE Ada} | {abc, 1.5} | 400 |  | [Ada] |
| ✅ passes | Acting id past 64 bits | {PUT Bob, DELETE Ada} | 99999999999999999999 | 400 |  | [Ada] |
