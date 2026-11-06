## R4: Warns about an unexpected status or a failure

| Scenario | Outcome | Warning? |
| --- | --- | --- |
| Expected status | [request: POST /speakers, expects: 400, answered: 400] |  |
| Other status | [request: POST /speakers, expects: 400, answered: 500] | Warm-up POST /speakers answered 500, expected 400 |
| No answer | [request: POST /speakers, expects: 400, failed: timed out] | Warm-up POST /speakers failed: timed out |
