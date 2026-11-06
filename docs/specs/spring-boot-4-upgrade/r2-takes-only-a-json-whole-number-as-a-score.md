## R2: Takes only a JSON whole number as a score

Ratings R2. The rating body is a map, so Jackson hands over the JSON type as it is and
never coerces it. Each row rates the talk as reviewer 7.


| Result | Scenario | Score | Status? | Message? |
| --- | --- | --- | --- | --- |
| ✅ passes | Whole number | 3 | 200 |  |
| ✅ passes | Decimal | 3.0 | 400 | score must be a whole number |
| ✅ passes | Exponent | 3e0 | 400 | score must be a whole number |
| ✅ passes | Text | "3" | 400 | score must be a whole number |
| ✅ passes | Boolean | true | 400 | score must be a whole number |
| ✅ passes | Null | null | 400 | score must be a whole number |
| ✅ passes | Array | [3] | 400 | score must be a whole number |
| ✅ passes | Out of range | 6 | 400 | score must be 1-5 |
