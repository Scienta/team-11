## R4: Names the main speaker, then the co-speakers as added

Talk A is put in R1 S1. Its co-speakers are added in the order of the list, before A is
accepted. Ada Lovelace, Barbara Liskov and Grace Hopper are registered speakers.


| Result | Scenario | Main Speaker | Co-speakers In Order Added | Speakers? |
| --- | --- | --- | --- | --- |
| ✅ passes | Main speaker only | Grace Hopper | [] | [Grace Hopper] |
| ✅ passes | One co-speaker | Grace Hopper | [Ada Lovelace] | [Grace Hopper, Ada Lovelace] |
| ✅ passes | Not in alphabetic order | Grace Hopper | [Barbara Liskov, Ada Lovelace] | [Grace Hopper, Barbara Liskov, Ada Lovelace] |
