## R2: Orders the rows by start, then by room

| Result | Scenario | Places In Any Order | Order? |
| --- | --- | --- | --- |
| ✅ passes | Three rooms, then a later slot | [R3/S1, R1/S2, R1/S1, R2/S1] | [R1/S1, R2/S1, R3/S1, R1/S2] |
| ✅ passes | Long slot before the last | [R1/S5, R3/S4, R2/S4] | [R2/S4, R3/S4, R1/S5] |
