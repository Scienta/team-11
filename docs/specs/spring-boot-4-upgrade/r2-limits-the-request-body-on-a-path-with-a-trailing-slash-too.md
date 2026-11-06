## R2: Limits the request body on a path with a trailing slash too

The trailing-slash filter runs before the 1 MB body limit, and the limit still applies.
Each request is PUT /talks/$talk/status/ with the decision WAITLISTED, padded with an
unknown field to the body size. A chunked body has no Content-Length.


| Result | Scenario | Body Size (bytes) | Chunked? | Status? |
| --- | --- | --- | --- | --- |
| ✅ passes | Exactly 1 MB | 1048576 | false | 200 |
| ✅ passes | One byte more | 1048577 | false | 413 |
| ✅ passes | One byte more | 1048577 | true | 413 |
