## R6: Registers one of the same email sent at the same time

All copies of a row are sent at the same time to one accepted workshop with no attendees.
Every second copy writes the email in capitals.


| Result | Scenario | Copies | Registered? | Refused? |
| --- | --- | --- | --- | --- |
| ✅ passes | A double-click | 2 | 1 | 1 |
| ✅ passes | Many copies | 10 | 1 | 9 |
