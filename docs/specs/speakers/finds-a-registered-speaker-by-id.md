## Finds a registered speaker by ID

Each row registers one speaker first, who gets ID 1.


| Result | Scenario | Speaker ID | Status? |
| --- | --- | --- | --- |
| ✅ passes | Registered | 1 | 200 |
| ✅ passes | Not registered | 2 | 404 |
| ✅ passes | Not a number | abc | 400 |
