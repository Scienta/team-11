## R1, R5: Refuses a co-speaker with 3 talks that count on a talk in the running

Ada first gets her own talks and her co-speaker places, each in the status given. Then
another speaker's talk gets the status in Target Talk, and Ada is added to it as co-speaker.
A blank message is not checked. A target talk that is rejected or withdrawn keeps the
answer of Co-speakers R9: the product owner confirmed it for this change request.


| Result | Scenario | Own Talks | Co-speaker Talks | Target Talk | Limit | Status? | Message? |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | Two of her own | [SUBMITTED, SUBMITTED] | [] | {SUBMITTED, WAITLISTED, ACCEPTED} | 3 | 200 |  |
| ✅ passes | Three of her own | [SUBMITTED, SUBMITTED, SUBMITTED] | [] | {SUBMITTED, WAITLISTED, ACCEPTED} | 3 | 409 | this speaker already has 3 talks |
| ✅ passes | Two of her own and one as co-speaker | [SUBMITTED, SUBMITTED] | [WAITLISTED] | SUBMITTED | 3 | 409 | this speaker already has 3 talks |
| ✅ passes | One of her own talks rejected | [SUBMITTED, SUBMITTED, REJECTED] | [] | SUBMITTED | 3 | 200 |  |
| ✅ passes | Her co-speaker talk withdrawn | [SUBMITTED, SUBMITTED] | [WITHDRAWN] | SUBMITTED | 3 | 200 |  |
| ✅ passes | Target talk out of the running | [SUBMITTED, SUBMITTED, SUBMITTED] | [] | {REJECTED, WITHDRAWN} | 3 | 409 | a withdrawn or rejected talk can't be changed |
