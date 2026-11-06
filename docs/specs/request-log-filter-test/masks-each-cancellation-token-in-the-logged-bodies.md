## Masks each cancellation token in the logged bodies

Workshop registration R10: the 201 of a registration returns a cancellation token, and the
DELETE that unregisters sends it back. Each row sends the body as the request and gets the
same body as the response.


| Result | Scenario | Body | Logged Body? |
| --- | --- | --- | --- |
| ✅ passes | Token in a 201 | {"id":1,"cancellationToken":"kQ9-x_Z2"} | {"id":1,"cancellationToken":"***"} |
| ✅ passes | Token in a DELETE | {"token":"kQ9-x_Z2"} | {"token":"***"} |
| ✅ passes | Spaces around the colon | {"token" : "kQ9-x_Z2"} | {"token" : "***"} |
| ✅ passes | Empty token | {"token":""} | {"token":"***"} |
| ✅ passes | Token that is not text | {"token":42} | {"token":42} |
| ✅ passes | Another field ending in token | {"csrfToken":"kQ9-x_Z2"} | {"csrfToken":"kQ9-x_Z2"} |
