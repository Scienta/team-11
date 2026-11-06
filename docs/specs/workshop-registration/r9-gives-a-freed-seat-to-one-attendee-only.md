## R9: Gives a freed seat to one attendee only

Each row fills an accepted workshop with 20 attendees and unregisters one. Then all the
new registrations of the row are sent at the same time, each with a different email.


| Result | Scenario | Registering At Once | Registered? | Refused? | Listed? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | Two for one seat | 2 | 1 | 1 | 20 |
| ✅ passes | Ten for one seat | 10 | 1 | 9 | 20 |
