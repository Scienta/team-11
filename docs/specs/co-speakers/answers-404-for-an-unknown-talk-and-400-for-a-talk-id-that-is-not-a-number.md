## Answers 404 for an unknown talk and 400 for a talk id that is not a number

| Scenario | Method | Talk | Status? |
| --- | --- | --- | --- |
| Unknown talk | {PUT, DELETE} | 2147483647 | 404 |
| Talk id that is not a number | {PUT, DELETE} | abc | 400 |
