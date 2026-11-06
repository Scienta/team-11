## R10: Unregisters only with the registration's email

Each row registers ada@example.com for an accepted workshop, and her registration gets
ID 1. Then it sends DELETE /workshops/1/registrations/1 with the body. A blank body sends
no body at all. The list is the workshop's list after the request.


| Result | Scenario | Body | Status? | Listed After? |
| --- | --- | --- | --- | --- |
| ✅ passes | Ada's email | {{"email": "ada@example.com"}, {"email": "ADA@Example.com"}} | 204 | [] |
| ✅ passes | Another email | {"email": "grace@example.com"} | 404 | [ada@example.com] |
| ✅ passes | Body without email | {} | 404 | [ada@example.com] |
| ✅ passes | No body |  | 400 | [ada@example.com] |
| ✅ passes | Body that is not JSON | {"email": | 400 | [ada@example.com] |
