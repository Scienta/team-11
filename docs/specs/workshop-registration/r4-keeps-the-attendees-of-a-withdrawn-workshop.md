## R4: Keeps the attendees of a withdrawn workshop

Each row registers the attendees for an accepted workshop, then its speaker withdraws it.


| Result | Scenario | Registered Before | Listed After Withdrawal? |
| --- | --- | --- | --- |
| ✅ passes | Two attendees | [ada@example.com, grace@example.com] | [ada@example.com, grace@example.com] |
| ✅ passes | Nobody registered | [] | [] |
