## Accepts a talk only with a real title

A blank cell sends JSON null as the title. Each row is the first talk of a newly
registered speaker, a PRESENTATION at level BEGINNER.


| Scenario | Title | Status? |
| --- | --- | --- |
| A real title | Legacy is a feeling | 201 |
| Empty | "" | 400 |
| Only spaces | &#x2423;&#x2423;&#x2423; | 400 |
| No title |  | 400 |
