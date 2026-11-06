## R2: Takes registrations only for a workshop

Each row submits and accepts one talk in the given format, which gets ID 1. Talk 2 does not
exist. A talk of another format refuses a registration like a closed workshop, and its list
of attendees is empty.


| Result | Scenario | Format | Talk ID | Register? | List? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | A workshop | WORKSHOP | 1 | 201 | 200 |
| ✅ passes | Another format | {LIGHTNING_10, LIGHTNING_20, PRESENTATION} | 1 | 409 | 200 |
| ✅ passes | No such talk | WORKSHOP | 2 | 404 | 404 |
