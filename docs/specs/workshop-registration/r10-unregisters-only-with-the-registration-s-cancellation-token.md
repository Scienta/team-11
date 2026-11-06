## R10: Unregisters only with the registration's cancellation token

Each row registers Ada and then Grace for an accepted workshop, so Ada's registration gets
ID 1. Each 201 returns a cancellation token. Then the row sends DELETE
/workshops/1/registrations/1 with the body. In a body, <token of Ada> stands for the token
from Ada's 201, and <token of Ada in capitals> for the same token in capitals. The list is
the workshop's list after the request.


| Result | Scenario | Body | Status? | Listed After? |
| --- | --- | --- | --- | --- |
| ✅ passes | Ada's token | {"token": "<token of Ada>"} | 204 | [grace@example.com] |
| ✅ passes | Grace's token | {"token": "<token of Grace>"} | 404 | [ada@example.com, grace@example.com] |
| ✅ passes | Ada's token in capitals | {"token": "<token of Ada in capitals>"} | 404 | [ada@example.com, grace@example.com] |
| ✅ passes | Ada's email | {"email": "ada@example.com"} | 404 | [ada@example.com, grace@example.com] |
| ✅ passes | Body without token | {} | 404 | [ada@example.com, grace@example.com] |
| ✅ passes | Token that is not text | {"token": 42} | 404 | [ada@example.com, grace@example.com] |
| ✅ passes | No body | no body | 400 | [ada@example.com, grace@example.com] |
| ✅ passes | Body that is not JSON | {"token": | 400 | [ada@example.com, grace@example.com] |
