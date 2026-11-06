## R8: Removes a co-speaker, never the main speaker

Removing a registered speaker who is not a co-speaker changes nothing, so a double-click
on remove does not fail. A blank message is not checked.


| Result | Scenario | Co-speakers before | Removed | Status? | Message? | Co-speakers after? |
| --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | A co-speaker | [Ada, Bob] | Ada | 200 |  | [Bob] |
| ✅ passes | Not a co-speaker | [Ada] | Bob | 200 |  | [Ada] |
| ✅ passes | The main speaker | [Ada] | Main | 409 | the main speaker can't be removed | [Ada] |
| ✅ passes | An unregistered speaker | [Ada] | Nobody | 404 | speaker not found | [Ada] |
| ✅ passes | No speaker has id 0 or below | [Ada] | {0, -1} | 404 | speaker not found | [Ada] |
| ✅ passes | Speaker id that is not a number | [Ada] | abc | 400 |  | [Ada] |
| ✅ passes | Speaker id past 64 bits | [Ada] | 99999999999999999999 | 400 |  | [Ada] |
