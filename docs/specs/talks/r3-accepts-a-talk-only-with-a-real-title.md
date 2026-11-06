## R3: Accepts a talk only with a real title

A blank cell sends JSON null as the title. Each row is the first talk of a newly
registered speaker, a PRESENTATION at level BEGINNER. A cell such as (200 × x) stands
for the letter x written 200 times. A character is one Unicode code point, so the emoji
😀 counts once, although JSON and Java count it as two UTF-16 units.


| Result | Scenario | Title | Status? |
| --- | --- | --- | --- |
| ✅ passes | A real title | Legacy is a feeling | 201 |
| ✅ passes | Empty | "" | 400 |
| ✅ passes | Only spaces | &#x2423;&#x2423;&#x2423; | 400 |
| ✅ passes | No title |  | 400 |
| ✅ passes | 200 characters | (200 × x) | 201 |
| ✅ passes | 201 characters | (201 × x) | 400 |
| ✅ passes | 200 emoji characters | (200 × 😀) | 201 |
| ✅ passes | 201 emoji characters | (201 × 😀) | 400 |
