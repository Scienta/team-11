## R9: Unregisters a registration once, from its own workshop

Each row has two workshops: workshop 1 in the given status, and workshop 2 accepted. Ada
registers for workshop 1 before it gets its status, and her registration gets ID 1. Talk
9 does not exist. The list is the list of workshop 1 after the request.


| Result | Scenario | Workshop 1 Status | Unregistered Before | Workshop ID | Registration ID | Status? | Listed After? |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | Ada unregisters | {ACCEPTED, WITHDRAWN} | false | 1 | 1 | 204 | [] |
| ✅ passes | A second time | ACCEPTED | true | 1 | 1 | 404 | [] |
| ✅ passes | Path of other workshop | ACCEPTED | false | 2 | 1 | 404 | [ada@example.com] |
| ✅ passes | No such registration | ACCEPTED | false | 1 | 2 | 404 | [ada@example.com] |
| ✅ passes | No such workshop | ACCEPTED | false | 9 | 1 | 404 | [ada@example.com] |
