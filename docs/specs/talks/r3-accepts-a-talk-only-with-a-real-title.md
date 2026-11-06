## R3: Accepts a talk only with a real title

A blank cell sends JSON null as the title. Each row is the first talk of a newly
registered speaker, a PRESENTATION at level BEGINNER.


| Result | Scenario | Title | Status? |
| --- | --- | --- | --- |
| ✅ passes | A real title | Legacy is a feeling | 201 |
| ✅ passes | Empty | "" | 400 |
| ✅ passes | Only spaces | &#x2423;&#x2423;&#x2423; | 400 |
| ✅ passes | No title |  | 400 |
