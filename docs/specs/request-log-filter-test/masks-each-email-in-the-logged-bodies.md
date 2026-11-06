## Masks each email in the logged bodies

Each row sends the body as the request and gets the same body as the response. Both logged
bodies must show the masked form. The mask keeps the first character and the domain, so a
person can still match a log line with a harness transcript.


| Result | Scenario | Body | Logged Body? |
| --- | --- | --- | --- |
| ✅ passes | One email | {"email":"ada@example.com"} | {"email":"a***@example.com"} |
| ✅ passes | Email in capitals | {"email":"ADA@Example.com"} | {"email":"A***@Example.com"} |
| ✅ passes | One letter before the @ | {"email":"a@example.com"} | {"email":"a***@example.com"} |
| ✅ passes | Two emails | [{"email":"ada@example.com"},{"email":"grace@example.org"}] | [{"email":"a***@example.com"},{"email":"g***@example.org"}] |
| ✅ passes | Domain without a dot | {"email":"ada@example"} | {"email":"a***@example"} |
| ✅ passes | No @ | {"email":"not-an-email"} | {"email":"not-an-email"} |
| ✅ passes | Nothing before the @ | {"email":"@example.com"} | {"email":"@example.com"} |
