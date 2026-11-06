## R8: Removes a co-speaker, never the main speaker

Removing a registered speaker who is not a co-speaker changes nothing, so a double-click
on remove does not fail.


| Scenario | Co-speakers before | Removed | Status? | Co-speakers after? |
| --- | --- | --- | --- | --- |
| A co-speaker | [Ada, Bob] | Ada | 200 | [Bob] |
| Not a co-speaker | [Ada] | Bob | 200 | [Ada] |
| The main speaker | [Ada] | Main | 409 | [Ada] |
| An unregistered speaker | [Ada] | Nobody | 404 | [Ada] |
| Speaker id that is not a number | [Ada] | abc | 400 | [Ada] |
