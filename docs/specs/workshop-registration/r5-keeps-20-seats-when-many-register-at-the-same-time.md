## R5: Keeps 20 seats when many register at the same time

All registrations of a row are sent at the same time, each with a different email, to one
accepted workshop with no attendees. The service uses at most 5 database connections.


| Result | Scenario | Registering At Once | Seats | Registered? | Refused? | Listed? |
| --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | Exactly the seats | 20 | 20 | 20 | 0 | 20 |
| ✅ passes | More than the seats | 30 | 20 | 20 | 10 | 20 |
