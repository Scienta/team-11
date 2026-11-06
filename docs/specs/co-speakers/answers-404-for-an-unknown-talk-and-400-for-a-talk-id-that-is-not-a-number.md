## Answers 404 for an unknown talk and 400 for a talk id that is not a number

| Result | Scenario | Method | Talk | Status? |
| --- | --- | --- | --- | --- |
| ✅ passes | Unknown talk | {PUT, DELETE} | 2147483647 | 404 |
| ✅ passes | No talk has id 0 or below | {PUT, DELETE} | {0, -1} | 404 |
| ✅ passes | Talk id that is not a number | {PUT, DELETE} | abc | 400 |
| ✅ passes | Talk id past 64 bits | {PUT, DELETE} | 99999999999999999999 | 400 |
