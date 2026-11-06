## R1, R2, R3, R5: Adds another registered speaker, at most two

Main is the talk's main speaker, Ada, Bob and Cy are other registered speakers, and Nobody
is a speaker id that is not registered. The co-speakers after are read from GET /talks/{id}.
A blank message is not checked.


| Result | Scenario | Co-speakers before | Added | Status? | Message? | Co-speakers after? |
| --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | R1: first co-speaker | [] | Ada | 200 |  | [Ada] |
| ✅ passes | R1: second, in the order added | [Ada] | Bob | 200 |  | [Ada, Bob] |
| ✅ passes | R2: a third co-speaker | [Ada, Bob] | Cy | 409 | a talk has at most 2 co-speakers | [Ada, Bob] |
| ✅ passes | R5: already a co-speaker | [Ada] | Ada | 200 |  | [Ada] |
| ✅ passes | R5: already one, at the limit | [Ada, Bob] | Bob | 200 |  | [Ada, Bob] |
| ✅ passes | R3: the main speaker | [] | Main | 400 | the main speaker can't be a co-speaker | [] |
| ✅ passes | R1: an unregistered speaker | [] | Nobody | 404 | speaker not found | [] |
| ✅ passes | No speaker has id 0 or below | [] | {0, -1} | 404 | speaker not found | [] |
| ✅ passes | Speaker id that is not a number | [] | abc | 400 |  | [] |
| ✅ passes | Speaker id past 64 bits | [] | 99999999999999999999 | 400 |  | [] |
