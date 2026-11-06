## R5: Refuses a second speaker with the same email

Each row registers a second speaker after a first one is registered.


| Result | Scenario | First Email | Second Email | Status? |
| --- | --- | --- | --- | --- |
| ✅ passes | Same address in any letter case | ada@example.com | {ada@example.com, ADA@Example.com} | 409 |
| ✅ passes | Another address | ada@example.com | grace@example.com | 201 |
