## R2: Answers a path with a trailing slash like the path without it

Speakers R7 names /speakers/ and /speakers/{id}/. Spring Boot 2.7 matched the slash on
every path, so every path keeps doing so. $speaker and $talk are a registered speaker
and their talk, $other another registered speaker and $email a new address.


| Result | Scenario | Method | Path | Body | Status? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | Register a speaker | POST | /speakers/ | {"name":"Grace Hopper","email":"$email"} | 201 |
| ✅ passes | Read a speaker | GET | /speakers/$speaker/ |  | 200 |
| ✅ passes | PUT is gone | PUT | /speakers/$speaker/ | {"name":"Grace Hopper","email":"$email"} | 405 |
| ✅ passes | A speaker's talks | GET | /speakers/$speaker/talks/ |  | 200 |
| ✅ passes | Submit a talk | POST | /talks/ | {"title":"T","format":"PRESENTATION","level":"BEGINNER","speakerId":$speaker} | 201 |
| ✅ passes | Read a talk | GET | /talks/$talk/ |  | 200 |
| ✅ passes | List the talks | GET | /talks/ |  | 200 |
| ✅ passes | List without withdrawn | GET | /talks/?includeWithdrawn=false |  | 200 |
| ✅ passes | Rate a talk | PUT | /talks/$talk/ratings/7/ | {"score":3} | 200 |
| ✅ passes | Decide on a talk | PUT | /talks/$talk/status/ | {"status":"WAITLISTED"} | 200 |
| ✅ passes | Withdraw a talk | POST | /talks/$talk/withdraw/ | {"speakerId":$speaker} | 200 |
| ✅ passes | Add a co-speaker | PUT | /talks/$talk/co-speakers/$other/ |  | 200 |
| ✅ passes | Remove a co-speaker | DELETE | /talks/$talk/co-speakers/$other/ |  | 200 |
