## Refuses a text field that holds a NUL character

Speaker 1 is registered. The text is written as it stands in the JSON body, so \u0000 is
the JSON escape for the NUL character. The database cannot store that character.


| Result | Scenario | Text Field | Text | Status? |
| --- | --- | --- | --- | --- |
| ✅ passes | Ordinary text | {speaker name, speaker bio, talk title} | Ada Lovelace | 201 |
| ✅ passes | NUL character | {speaker name, speaker bio, talk title} | Ada\u0000Lovelace | 400 |
