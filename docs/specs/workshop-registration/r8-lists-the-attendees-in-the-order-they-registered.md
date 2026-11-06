## R8: Lists the attendees in the order they registered

Each row registers the emails one by one, in the order of the list, for an accepted
workshop. The list shows each email as the attendee typed it.


| Result | Scenario | Registered In Order | Listed? |
| --- | --- | --- | --- |
| ✅ passes | Three attendees | [ada@example.com, grace@example.com, barbara@example.com] | [ada@example.com, grace@example.com, barbara@example.com] |
| ✅ passes | Email with capitals | [ADA@Example.com] | [ADA@Example.com] |
| ✅ passes | Nobody | [] | [] |
