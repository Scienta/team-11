## R9: Takes only true or false to leave out withdrawn talks

An empty value, as in /talks?includeWithdrawn=, counts as no value and lists all talks.


| Result | Scenario | Include Withdrawn | Response? |
| --- | --- | --- | --- |
| ✅ passes | True or false | {true, false} | 200 |
| ✅ passes | Another value | {yes, 1, TRUE} | 400 |
