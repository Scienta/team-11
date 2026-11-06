## R2: Puts a talk only for a body with a talk ID

Each row creates talk A first, an accepted presentation, so A has ID 1. There is no talk
999999999. Then it sends PUT /programme/R1/S1 with the body. A blank body sends no body.


| Result | Scenario | Body | Status? | Programme After? |
| --- | --- | --- | --- | --- |
| ✅ passes | Talk ID of A | {"talkId": 1} | 200 | [R1/S1: A] |
| ✅ passes | Other fields as well | {"talkId": 1, "room": "R2"} | 200 | [R1/S1: A] |
| ✅ passes | No body |  | 400 | [:] |
| ✅ passes | Body that is not JSON | {"talkId": | 400 | [:] |
| ✅ passes | Body that is a list | [1] | 400 | [:] |
| ✅ passes | No talk ID | {} | 400 | [:] |
| ✅ passes | Talk ID is null | {"talkId": null} | 400 | [:] |
| ✅ passes | Talk ID is a string | {"talkId": "1"} | 400 | [:] |
| ✅ passes | Talk ID with a fraction | {"talkId": 1.0} | 400 | [:] |
| ✅ passes | No such talk | {"talkId": 999999999} | 400 | [:] |
| ✅ passes | Talk ID 0 or below | {{"talkId": 0}, {"talkId": -1}} | 400 | [:] |
