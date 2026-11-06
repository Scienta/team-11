## Refuses a request body larger than 1 MB

Each request is PUT /talks/1/status with the decision WAITLISTED for submitted talk 1, padded
with an unknown field to the body size. 1 MB is 1048576 bytes. A fixed-length body carries
a Content-Length; a chunked body does not, so the service must count the bytes itself.


| Result | Scenario | Body Size (bytes) | Transfer | Status? |
| --- | --- | --- | --- | --- |
| ✅ passes | Exactly 1 MB | 1048576 | {fixed length, chunked} | 200 |
| ✅ passes | One byte more | 1048577 | {fixed length, chunked} | 413 |
| ✅ passes | Far more than 1MB | 10485760 | {fixed length, chunked} | 413 |
