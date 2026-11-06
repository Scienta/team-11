## R1, R2, R4: Refuses a new talk from a speaker with 3 talks that count

Ada first gets her own talks and her co-speaker places, each on a talk of another speaker,
each in the status given. Then Ada submits one more talk of her own.


| Result | Scenario | Own Talks | Co-speaker Talks | Limit | New Talk? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | Two of her own | [SUBMITTED, SUBMITTED] | [] | 3 | 201 |
| ✅ passes | Three of her own, each in the running | [SUBMITTED, WAITLISTED, ACCEPTED] | [] | 3 | 409 |
| ✅ passes | Two of her own and one as co-speaker | [SUBMITTED, SUBMITTED] | [SUBMITTED] | 3 | 409 |
| ✅ passes | One of her own and two as co-speaker | [SUBMITTED] | [WAITLISTED, ACCEPTED] | 3 | 409 |
| ✅ passes | One of her own talks rejected | [SUBMITTED, SUBMITTED, REJECTED] | [] | 3 | 201 |
| ✅ passes | One of her own talks withdrawn | [SUBMITTED, SUBMITTED, WITHDRAWN] | [] | 3 | 201 |
| ✅ passes | Her co-speaker talk rejected | [SUBMITTED, SUBMITTED] | [REJECTED] | 3 | 201 |
| ✅ passes | Her co-speaker talk withdrawn | [SUBMITTED, SUBMITTED] | [WITHDRAWN] | 3 | 201 |
