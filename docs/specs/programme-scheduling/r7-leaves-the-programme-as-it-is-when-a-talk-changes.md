## R7: Leaves the programme as it is when a talk changes

Every talk is an accepted presentation. The change happens after the programme before is set
up: the main speaker withdraws the talk, or a speaker becomes a co-speaker of the talk.


| Result | Scenario | Speakers | Programme Before | Change | Request | Status? | Programme After? |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | Talk withdrawn, looked up | [A: Ada] | [R1/S1: A] | A withdrawn | GET R1/S1 | 200 | [R1/S1: A] |
| ✅ passes | Talk withdrawn, taken out | [A: Ada] | [R1/S1: A] | A withdrawn | DELETE R1/S1 | 204 | [:] |
| ✅ passes | Speaker in two rooms after | [A: Ada, B: Bob] | [R1/S2: A, R2/S2: B] | Ada co-speaker of B | GET R2/S2 | 200 | [R1/S2: A, R2/S2: B] |
