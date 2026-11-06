## R4: Joins the speakers with a semicolon in one field

Each row is talk 7, "Legacy is a feeling", a PRESENTATION in R1 S1. The speakers come in the
order of the list: the main speaker first, then the co-speakers in the order they were added.


| Result | Scenario | Speakers | CSV Line? |
| --- | --- | --- | --- |
| ✅ passes | Main speaker only | [Grace Hopper] | S1,09:00,10:00,R1,7,Legacy is a feeling,PRESENTATION,Grace Hopper |
| ✅ passes | One co-speaker | [Grace Hopper, Ada Lovelace] | S1,09:00,10:00,R1,7,Legacy is a feeling,PRESENTATION,Grace Hopper; Ada Lovelace |
| ✅ passes | Two co-speakers | [Grace Hopper, Barbara Liskov, Ada Lovelace] | S1,09:00,10:00,R1,7,Legacy is a feeling,PRESENTATION,Grace Hopper; Barbara Liskov; Ada Lovelace |
