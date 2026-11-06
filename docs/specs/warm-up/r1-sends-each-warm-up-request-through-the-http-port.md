## R1: Sends each warm-up request through the HTTP port

Each row looks for the request's line in the request log (RequestLogFilter) after the start.
The service is new and its database is empty.


| Scenario | Method | Path | Request Body | Status? |
| --- | --- | --- | --- | --- |
| Speaker form that fails validation | POST | /speakers | {} | 400 |
| Unknown speaker | GET | /speakers/0 | "" | 404 |
| Talk form that fails validation | POST | /talks | {} | 400 |
| Unknown talk | GET | /talks/0 | "" | 404 |
| Ranking for a speaker with no talks | GET | /talks?speakerId=0&sort=rating | "" | 200 |
| Rating for an unknown talk | PUT | /talks/0/ratings/1 | {"score":3} | 404 |
