## R1: Accepts a reviewer from 1 to 9223372036854775807

Reviewers are not registered: the reviewer is the number at the end of
PUT /talks/{id}/ratings/{reviewerId}. Each row rates a newly submitted talk with score 4.
Open question for the product owner: is a reviewer number above 2147483647 realistic?


| Result | Scenario | Reviewer | Status? |
| --- | --- | --- | --- |
| ✅ passes | Lowest positive number | 1 | 200 |
| ✅ passes | Around the largest 32-bit | {2147483647, 2147483648} | 200 |
| ✅ passes | Largest 64-bit number | 9223372036854775807 | 200 |
| ✅ passes | One past the largest 64-bit | 9223372036854775808 | 400 |
| ✅ passes | Zero | 0 | 400 |
| ✅ passes | Negative number | -1 | 400 |
