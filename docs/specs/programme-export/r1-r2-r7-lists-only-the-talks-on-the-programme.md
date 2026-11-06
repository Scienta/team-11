## R1, R2, R7: Lists only the talks on the programme

The programme is set up with PUT, in the order of the cell. The other talks are not on the
programme: an accepted talk, a submitted talk, or a talk taken out again. The talks are
read from the JSON list and from the talkId column of the CSV.


| Result | Scenario | Programme | Other Talks | JSON Talks? | CSV Talks? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | Two talks | [R1/S1: A, R2/S3: B] | [:] | [A, B] | [A, B] |
| ✅ passes | Put in a later slot first | [R2/S3: B, R1/S1: A] | [:] | [A, B] | [A, B] |
| ✅ passes | Empty programme | [:] | [:] | [] | [] |
| ✅ passes | Talks not on the programme | [:] | [C: accepted, D: submitted, E: taken out] | [] | [] |
| ✅ passes | One of two accepted talks | [R1/S1: A] | [C: accepted] | [A] | [A] |
