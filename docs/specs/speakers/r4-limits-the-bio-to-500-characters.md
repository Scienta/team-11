## R4: Limits the bio to 500 characters

A character is one Unicode code point, so the emoji 😀 counts once, although JSON and Java
count it as two UTF-16 units. A bio is optional.


| Scenario | Bio | Status? |
| --- | --- | --- |
| No bio |  | 201 |
| At the limit | 500 × x | 201 |
| One past the limit | 501 × x | 400 |
| Emoji at the limit | 500 × 😀 | 201 |
| Emoji one past the limit | 501 × 😀 | 400 |
