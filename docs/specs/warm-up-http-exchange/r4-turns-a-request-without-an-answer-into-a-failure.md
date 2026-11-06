## R4: Turns a request without an answer into a failure

Each row sends GET /speakers/0 to a stand-in for the service on localhost. The production time-out is
5 s; here it is shorter, so that the silent row does not slow the build.


| Result | Scenario | Service | Time-out | Outcome? |
| --- | --- | --- | --- | --- |
| ✅ passes | Service answers | answers 404 | PT0.5S | Answered |
| ✅ passes | Service gives no answer | {nothing on the port, accepts and stays silent} | PT0.5S | Failed |
