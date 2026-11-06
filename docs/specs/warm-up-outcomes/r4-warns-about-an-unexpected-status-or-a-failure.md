## R4: Warns about an unexpected status or a failure

| Result | Scenario | Outcome | Warning? |
| --- | --- | --- | --- |
| ✅ passes | Expected status | [request: POST /speakers, expects: 400, answered: 400] |  |
| ✅ passes | Other status | [request: POST /speakers, expects: 400, answered: 500] | Warm-up POST /speakers answered 500, expected 400 |
| ✅ passes | No answer | [request: POST /speakers, expects: 400, failed: timed out] | Warm-up POST /speakers failed: timed out |
