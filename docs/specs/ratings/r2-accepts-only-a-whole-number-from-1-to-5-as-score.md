## R2: Accepts only a whole number from 1 to 5 as score

The score is the JSON value of "score" in the request body, written here as it is sent.
A blank cell sends a body without "score". Each row is the first rating of reviewer 1.


| Result | Scenario | Score | Status? |
| --- | --- | --- | --- |
| ✅ passes | A score on the scale | {1, 2, 3, 4, 5} | 200 |
| ✅ passes | Below the scale | 0 | 400 |
| ✅ passes | Above the scale | 6 | 400 |
| ✅ passes | Decimal number | 4.0 | 400 |
| ✅ passes | Number as text | "4" | 400 |
| ✅ passes | Yes or no | true | 400 |
| ✅ passes | No score |  | 400 |
