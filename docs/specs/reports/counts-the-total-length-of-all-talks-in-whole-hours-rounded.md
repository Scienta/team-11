## Counts the total length of all talks in whole hours, rounded

One speaker submits a talk in each format of the list. Half an hour rounds up.


| Result | Scenario | Formats | Total Hours? |
| --- | --- | --- | --- |
| ✅ passes | No talks | [] | 0 |
| ✅ passes | 185 minutes | [WORKSHOP, PRESENTATION, LIGHTNING_20] | 3 |
| ✅ passes | 210 minutes | [WORKSHOP, PRESENTATION, PRESENTATION] | 4 |
| ✅ passes | 160 minutes | [WORKSHOP, LIGHTNING_20, LIGHTNING_20] | 3 |
| ✅ passes | 150 minutes | [WORKSHOP, LIGHTNING_20, LIGHTNING_10] | 3 |
| ✅ passes | 140 minutes | [WORKSHOP, LIGHTNING_10, LIGHTNING_10] | 2 |
