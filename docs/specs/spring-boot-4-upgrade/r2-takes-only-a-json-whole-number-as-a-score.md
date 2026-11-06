## R2: Takes only a JSON whole number as a score

Ratings R2. The rating body is a map, so Jackson hands over the JSON type as it is and
never coerces it. Each row rates the talk as reviewer 7.


| Scenario | Score | Status? | Message? |
| --- | --- | --- | --- |
| Whole number | 3 | 200 |  |
| Decimal | 3.0 | 400 | score must be a whole number |
| Exponent | 3e0 | 400 | score must be a whole number |
| Text | "3" | 400 | score must be a whole number |
| Boolean | true | 400 | score must be a whole number |
| Null | null | 400 | score must be a whole number |
| Array | [3] | 400 | score must be a whole number |
| Out of range | 6 | 400 | score must be 1-5 |
