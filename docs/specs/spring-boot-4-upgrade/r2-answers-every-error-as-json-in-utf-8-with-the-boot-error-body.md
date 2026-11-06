## R2: Answers every error as JSON in UTF-8, with the Boot error body

As on Boot 2.7, an error has Content-Type application/json;charset=UTF-8, like a success,
and the fields timestamp, status, error, message and path. $speaker and $talk are a
registered speaker and their talk, $email a new address and $taken the speaker's address.
No row has a 500: no request gives one without a bug.


| Result | Scenario | Method | Path | Body | Status? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | Our own 400 | POST | /speakers | {"name":"Ada","email":"not-an-email"} | 400 |
| ✅ passes | Validation error | POST | /speakers | {"name":"","email":"$email"} | 400 |
| ✅ passes | Malformed JSON | POST | /speakers | {not json | 400 |
| ✅ passes | Path variable not a number | GET | /speakers/abc |  | 400 |
| ✅ passes | Not the main speaker | POST | /talks/$talk/withdraw | {"speakerId":999999} | 403 |
| ✅ passes | Unknown speaker | GET | /speakers/999999 |  | 404 |
| ✅ passes | Unknown path | GET | /nope |  | 404 |
| ✅ passes | Wrong method | DELETE | /speakers |  | 405 |
| ✅ passes | Email taken | POST | /speakers | {"name":"Ada","email":"$taken"} | 409 |
