## Logs a large body in a short time

The body has no @ and no line break: the slowest input for the email mask and for the
joining of lines. The filter logs the body as the request and as the response, and the
time limit is for both. The limit leaves room for a cold JIT on a shared CI runner. The
quadratic mask of 9458d53 took 16 s for 80 KB, so the rows still catch that.


| Result | Scenario | Character | Repeats | Time Limit (ms) |
| --- | --- | --- | --- | --- |
| ✅ passes | 80 KB of letters | A | 80000 | 2000 |
| ✅ passes | 2 MB of letters | A | 2000000 | 2000 |
| ✅ passes | 2 MB of spaces | &#x2423; | 2000000 | 2000 |
| ✅ passes | 2 MB of quotes | " | 2000000 | 2000 |
