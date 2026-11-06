## R9: Frees the seat when an attendee unregisters

Each row fills an accepted workshop with 20 attendees, then registers one more.


| Result | Scenario | Unregisters One First | Next Registration? | Listed? |
| --- | --- | --- | --- | --- |
| ✅ passes | An attendee unregisters | true | 201 | 20 |
| ✅ passes | Nobody unregisters | false | 409 | 20 |
