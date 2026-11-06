## R1: Registers an attendee only with a name and a real email

Each row registers for an accepted workshop. A blank cell leaves the field out of the
body, and null sends JSON null. The email rule is the same as for speakers.


| Result | Scenario | Name | Email | Status? |
| --- | --- | --- | --- | --- |
| ✅ passes | Name and email | Ada Lovelace | ada@example.com | 201 |
| ✅ passes | Empty name | "" | ada@example.com | 400 |
| ✅ passes | Name of spaces | &#x2423;&#x2423;&#x2423; | ada@example.com | 400 |
| ✅ passes | Name is null | null | ada@example.com | 400 |
| ✅ passes | No name |  | ada@example.com | 400 |
| ✅ passes | Not an email | Ada Lovelace | not-an-email | 400 |
| ✅ passes | Empty email | Ada Lovelace | "" | 400 |
| ✅ passes | Email is null | Ada Lovelace | null | 400 |
| ✅ passes | No email | Ada Lovelace |  | 400 |
