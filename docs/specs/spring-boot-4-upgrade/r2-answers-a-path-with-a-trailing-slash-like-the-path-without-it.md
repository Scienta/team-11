## R2: Answers a path with a trailing slash like the path without it

Speakers R7 names /speakers/ and /speakers/{id}/. Spring Boot 2.7 matched the slash on
every path, so every path keeps doing so. $speaker and $talk are a registered speaker
and their talk.


| Scenario | Method | Path | Status? |
| --- | --- | --- | --- |
| Register a speaker | POST | /speakers/ | 201 |
| Read a speaker | GET | /speakers/$speaker/ | 200 |
| Update a speaker | PUT | /speakers/$speaker/ | 200 |
| A speaker's talks | GET | /speakers/$speaker/talks/ | 200 |
| Submit a talk | POST | /talks/ | 201 |
| Read a talk | GET | /talks/$talk/ | 200 |
| List the talks | GET | /talks/ | 200 |
| Rate a talk | PUT | /talks/$talk/ratings/7/ | 200 |
