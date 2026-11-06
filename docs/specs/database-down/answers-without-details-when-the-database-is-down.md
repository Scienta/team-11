## Answers without details when the database is down

No database answers on the configured port. A request that starts a transaction fails
before its first query; a request without a transaction fails on its first query.


| Result | Scenario | Request | Status? | Message? |
| --- | --- | --- | --- | --- |
| ✅ passes | Request with a transaction | /talks/1 | 500 | the service could not complete the request |
| ✅ passes | Request without a transaction | /speakers/1 | 500 | the service could not complete the request |
