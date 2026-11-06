## R1: Sends each warm-up request through the HTTP port

Each row looks for the request's line in the request log (RequestLogFilter) after the start.
The service is new and its database is empty.


| Result | Scenario | Method | Path | Request Body | Status? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | Speaker form that fails validation | POST | /speakers | {} | 400 |
| ✅ passes | Unknown speaker | GET | /speakers/0 | "" | 404 |
| ✅ passes | Talk form that fails validation | POST | /talks | {} | 400 |
| ✅ passes | Unknown talk | GET | /talks/0 | "" | 404 |
| ✅ passes | Ranking for a speaker with no talks | GET | /talks?speakerId=0&sort=rating | "" | 200 |
| ✅ passes | Rating for an unknown talk | PUT | /talks/0/ratings/1 | {"score":3} | 404 |
