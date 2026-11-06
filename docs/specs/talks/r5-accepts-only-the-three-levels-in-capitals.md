## R5: Accepts only the three levels, in capitals

Each row is the first talk of a newly registered speaker, in the format PRESENTATION.


| Result | Scenario | Level | Status? |
| --- | --- | --- | --- |
| ✅ passes | A listed level | {BEGINNER, INTERMEDIATE, ADVANCED} | 201 |
| ✅ passes | Another level | EXPERT | 400 |
| ✅ passes | Lower case | beginner | 400 |
| ✅ passes | No level |  | 400 |
