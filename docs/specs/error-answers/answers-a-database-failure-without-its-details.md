## Answers a database failure without its details

The search term reaches the database as a parameter, and the database refuses the NUL
character (%00 in the URL). The answer has the usual error fields.


| Result | Scenario | Request | Status? | Message? |
| --- | --- | --- | --- | --- |
| ✅ passes | Search term with a NUL character | /speakers/search?q=Grace%00Hop | 500 | the service could not complete the request |
