## R1, R2, R3, R5: Adds another registered speaker, at most two

Main is the talk's main speaker, Ada, Bob and Cy are other registered speakers, and Nobody
is a speaker id that is not registered. The co-speakers after are read from GET /talks/{id}.


| Scenario | Co-speakers before | Added | Status? | Co-speakers after? |
| --- | --- | --- | --- | --- |
| R1: first co-speaker | [] | Ada | 200 | [Ada] |
| R1: second, in the order added | [Ada] | Bob | 200 | [Ada, Bob] |
| R2: a third co-speaker | [Ada, Bob] | Cy | 409 | [Ada, Bob] |
| R5: already a co-speaker | [Ada] | Ada | 200 | [Ada] |
| R5: already one, at the limit | [Ada, Bob] | Bob | 200 | [Ada, Bob] |
| R3: the main speaker | [] | Main | 409 | [] |
| R1: an unregistered speaker | [] | Nobody | 404 | [] |
| Speaker id that is not a number | [] | abc | 400 | [] |
