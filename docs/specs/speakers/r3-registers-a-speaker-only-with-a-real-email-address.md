## R3: Registers a speaker only with a real email address

A real address has one @, a local part, and a domain of at least two labels split by dots,
with no spaces.


| Scenario | Email | Status? |
| --- | --- | --- |
| Simple address | ada@example.com | 201 |
| Plus sign and subdomains | ada.lovelace+cfp@mail.example.co.uk | 201 |
| No @ | not-an-email | 400 |
| Domain with one label | ada@example | 400 |
| No local part | @example.com | 400 |
| Domain starts with a dot | ada@.com | 400 |
| Domain ends with a dot | ada@example. | 400 |
| Two dots in a row in the domain | ada@example..com | 400 |
| Two @ | ada@@example.com | 400 |
| Space in the local part | ada lovelace@example.com | 400 |
