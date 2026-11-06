## R3: Registers a speaker only with a real email address

A real address has one @, a local part, and a domain of at least two labels split by dots,
with no spaces. The limits on length come from RFC 5321. A blank cell sends JSON null as
the email. A cell such as (64 × a) stands for the letter a written 64 times.


| Result | Scenario | Email | Status? |
| --- | --- | --- | --- |
| ✅ passes | Simple address | ada@example.com | 201 |
| ✅ passes | Plus sign and subdomains | ada.lovelace+cfp@mail.example.co.uk | 201 |
| ✅ passes | No @ | not-an-email | 400 |
| ✅ passes | Domain with one label | ada@example | 400 |
| ✅ passes | No local part | @example.com | 400 |
| ✅ passes | Domain starts with a dot | ada@.com | 400 |
| ✅ passes | Domain ends with a dot | ada@example. | 400 |
| ✅ passes | Two dots in a row in the domain | ada@example..com | 400 |
| ✅ passes | Two @ | ada@@example.com | 400 |
| ✅ passes | Space in the local part | ada lovelace@example.com | 400 |
| ✅ passes | Local part of 64 characters | (64 × a)@example.com | 201 |
| ✅ passes | Local part of 65 characters | (65 × a)@example.com | 400 |
| ✅ passes | Domain of 255 characters | ada@(251 × e).com | 201 |
| ✅ passes | Domain of 256 characters | ada@(252 × e).com | 400 |
| ✅ passes | Empty | "" | 400 |
| ✅ passes | Only spaces | &#x2423;&#x2423;&#x2423; | 400 |
| ✅ passes | No email |  | 400 |
