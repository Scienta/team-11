## R3, R5: Refuses an incomplete talk with the reason

The reason is the message field of the error body. Speaker "registered" is a newly
registered speaker; an empty Speaker cell leaves speakerId out.


| Result | Scenario | Title | Format | Level | Speaker | Status? | Message? |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | No title |  | PRESENTATION | BEGINNER | registered | 400 | title is required |
| ✅ passes | Unknown format | Legacy | KEYNOTE | BEGINNER | registered | 400 | format must be one of [WORKSHOP, PRESENTATION, LIGHTNING_20, LIGHTNING_10] |
| ✅ passes | No format | Legacy |  | BEGINNER | registered | 400 | format must be one of [WORKSHOP, PRESENTATION, LIGHTNING_20, LIGHTNING_10] |
| ✅ passes | Unknown level | Legacy | PRESENTATION | EXPERT | registered | 400 | unknown level |
| ✅ passes | No speaker | Legacy | PRESENTATION | BEGINNER |  | 400 | speakerId is required |
| ✅ passes | Unknown speaker | Legacy | PRESENTATION | BEGINNER | 999999 | 400 | speaker 999999 does not exist |
