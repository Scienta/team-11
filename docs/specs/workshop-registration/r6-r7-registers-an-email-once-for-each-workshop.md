## R6, R7: Registers an email once for each workshop

Each row registers ada@example.com for workshop 1 first. Workshops 1 and 2 are both
accepted.


| Result | Scenario | Second Email | Second Workshop | Status? |
| --- | --- | --- | --- | --- |
| ✅ passes | Same email, any letter case | {ada@example.com, ADA@Example.com} | 1 | 409 |
| ✅ passes | Another email | grace@example.com | 1 | 201 |
| ✅ passes | Same email, other workshop | ada@example.com | 2 | 201 |
