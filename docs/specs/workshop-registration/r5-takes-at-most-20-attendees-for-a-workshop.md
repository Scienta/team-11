## R5: Takes at most 20 attendees for a workshop

Each row registers attendees with different emails one by one, then registers one more.


| Result | Scenario | Already Registered | Status? | Listed? |
| --- | --- | --- | --- | --- |
| ✅ passes | One seat left | 19 | 201 | 20 |
| ✅ passes | No seat left | 20 | 409 | 20 |
