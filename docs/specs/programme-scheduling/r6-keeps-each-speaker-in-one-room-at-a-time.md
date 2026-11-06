## R6: Keeps each speaker in one room at a time

The speakers give each talk's main speaker first, then its co-speakers. Every talk is an
accepted presentation. Ada, Bob and Cy are three registered speakers.


| Result | Scenario | Speakers | Programme Before | Request | Status? | Programme After? |
| --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | Same main speaker | [A: Ada, B: Ada] | [R1/S2: A] | PUT B R2/S2 | 409 | [R1/S2: A] |
| ✅ passes | Co-speaker of the new talk | [A: Ada, B: Bob with co-speaker Ada] | [R1/S2: A] | PUT B R2/S2 | 409 | [R1/S2: A] |
| ✅ passes | Co-speaker of the scheduled talk | [A: Bob with co-speaker Ada, B: Ada] | [R1/S2: A] | PUT B R2/S2 | 409 | [R1/S2: A] |
| ✅ passes | Co-speaker of both talks | [A: Bob with co-speaker Ada, B: Cy with co-speaker Ada] | [R1/S2: A] | PUT B R2/S2 | 409 | [R1/S2: A] |
| ✅ passes | Different speakers | [A: Ada, B: Bob] | [R1/S2: A] | PUT B R2/S2 | 200 | [R1/S2: A, R2/S2: B] |
| ✅ passes | Same main speaker, another slot | [A: Ada, B: Ada] | [R1/S2: A] | PUT B R2/S3 | 200 | [R1/S2: A, R2/S3: B] |
