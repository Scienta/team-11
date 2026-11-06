## Logs a large body in a short time

The body has no @ and no line break: the slowest input for the email mask and for the
joining of lines. The filter logs the body as the request and as the response, and the
time limit is for both.


| Result | Scenario | Character | Repeats | Time Limit (ms) |
| --- | --- | --- | --- | --- |
| ✅ passes | 80 KB of letters | A | 80000 | 200 |
| ✅ passes | 2 MB of letters | A | 2000000 | 200 |
| ✅ passes | 2 MB of spaces | &#x2423; | 2000000 | 200 |
