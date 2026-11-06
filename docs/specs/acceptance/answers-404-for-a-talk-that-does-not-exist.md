## Answers 404 for a talk that does not exist

| Result | Scenario | Request | Response? |
| --- | --- | --- | --- |
| ✅ passes | Decision or withdrawal | {PUT /talks/99/status {"status":"REJECTED"}, POST /talks/99/withdraw {"speakerId":1}} | 404 |
