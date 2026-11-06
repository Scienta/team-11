## R5: Refuses a second speaker with the same email

Each row registers a second speaker after a first one is registered.


| Scenario | First Email | Second Email | Status? |
| --- | --- | --- | --- |
| Same address in any letter case | ada@example.com | {ada@example.com, ADA@Example.com} | 409 |
| Another address | ada@example.com | grace@example.com | 201 |
