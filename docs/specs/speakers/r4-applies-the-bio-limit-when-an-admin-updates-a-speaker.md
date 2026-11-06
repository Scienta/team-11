## R4: Applies the bio limit when an admin updates a speaker

The admin update is PUT /speakers/{id}. Each row updates the bio of a registered speaker
and keeps the name and the email.


| Scenario | Bio | Status? |
| --- | --- | --- |
| Emoji at the limit | 500 × 😀 | 200 |
| One past the limit | 501 × x | 400 |
