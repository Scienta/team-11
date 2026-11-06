## R3: Registers a speaker only with a name

A blank cell sends JSON null as the name. Each row registers with a real email.


| Result | Scenario | Name | Status? |
| --- | --- | --- | --- |
| ✅ passes | A real name | Grace Hopper | 201 |
| ✅ passes | Empty | "" | 400 |
| ✅ passes | Only spaces | &#x2423;&#x2423;&#x2423; | 400 |
| ✅ passes | No name |  | 400 |
