## R4: Puts a workshop only in the long slot

Each row puts accepted talk A of the format in room R1 and the slot. S4 is the long slot.


| Result | Scenario | Format | Slot | Status? | Saved? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | Workshop in the long slot | WORKSHOP | S4 | 200 | true |
| ✅ passes | Workshop in a short slot | WORKSHOP | {S1, S2, S3, S5} | 409 | false |
| ✅ passes | Other format in any slot | {LIGHTNING_10, LIGHTNING_20, PRESENTATION} | {S1, S2, S3, S4, S5} | 200 | true |
