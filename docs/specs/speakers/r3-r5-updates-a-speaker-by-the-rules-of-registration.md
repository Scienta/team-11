## R3, R5: Updates a speaker by the rules of registration

The admin update is PUT /speakers/{id}. Each row registers grace@example.com (ID 1) and
ada@example.com (ID 2) first. The reason is the message field of the error body.


| Scenario | Speaker ID | Name | Email | Status? | Message? |
| --- | --- | --- | --- | --- | --- |
| Own email in another case | 1 | Grace Hopper | GRACE@example.com | 200 |  |
| Another speaker's email | 1 | Grace Hopper | ADA@example.com | 409 | email already registered |
| Not an email | 1 | Grace Hopper | not-an-email | 400 | invalid email |
| No name | 1 |  | grace@example.com | 400 | name is required |
| Unknown speaker | 3 | Grace Hopper | grace@example.com | 404 | speaker not found |
