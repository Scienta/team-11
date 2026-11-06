## R6, R9: Registers an email again after it unregistered

Each row registers ada@example.com for an accepted workshop first, then registers the
same email again.


| Result | Scenario | Unregisters First | Status? |
| --- | --- | --- | --- |
| ✅ passes | Ada unregistered | true | 201 |
| ✅ passes | Ada still registered | false | 409 |
