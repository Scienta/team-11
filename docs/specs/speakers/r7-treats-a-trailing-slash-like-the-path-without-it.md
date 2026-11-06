## R7: Treats a trailing slash like the path without it

Each row registers speaker 1 first. Every request then carries the registration of
another speaker as its body, which GET ignores.


| Scenario | Method | Path | Status? |
| --- | --- | --- | --- |
| Look up a speaker | GET | {/speakers/1, /speakers/1/} | 200 |
| Register a speaker | POST | {/speakers, /speakers/} | 201 |
