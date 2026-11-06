## R4: Applies the bio limit when an admin updates a speaker

The admin update is PUT /speakers/{id}. Each row updates the bio of a registered speaker
and keeps the name and the email.


| Result | Scenario | Bio | Status? |
| --- | --- | --- | --- |
| ✅ passes | Emoji at the limit | 500 × 😀 | 200 |
| ✅ passes | One past the limit | 501 × x | 400 |
