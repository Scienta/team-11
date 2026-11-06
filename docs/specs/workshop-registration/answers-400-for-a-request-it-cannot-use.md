## Answers 400 for a request it cannot use

Each row submits and accepts one workshop first, which gets ID 1; there is no talk 2. A
blank body sends no body at all. The body is checked before the workshop, so an unknown
workshop with an unusable body gives 400.


| Result | Scenario | Method | Workshop ID | Body | Status? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | No body | POST | 1 |  | 400 |
| ✅ passes | Body that is not JSON | POST | 1 | {"name": "Ada Lovelace" | 400 |
| ✅ passes | Body that is a list | POST | 1 | [] | 400 |
| ✅ passes | ID that is not a number | {POST, GET} | abc | {"name": "Ada Lovelace", "email": "ada@example.com"} | 400 |
| ✅ passes | Unknown workshop, no name | POST | 2 | {"email": "ada@example.com"} | 400 |
