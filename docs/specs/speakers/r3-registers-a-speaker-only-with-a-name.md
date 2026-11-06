## R3: Registers a speaker only with a name

A blank cell sends JSON null as the name. Each row registers with a real email.


| Scenario | Name | Status? |
| --- | --- | --- |
| A real name | Grace Hopper | 201 |
| Empty | "" | 400 |
| Only spaces | &#x2423;&#x2423;&#x2423; | 400 |
| No name |  | 400 |
