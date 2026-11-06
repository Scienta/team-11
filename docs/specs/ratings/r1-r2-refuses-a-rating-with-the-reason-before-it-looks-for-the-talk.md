## R1, R2: Refuses a rating with the reason, before it looks for the talk

The reason is the message field of the error body. Talk "submitted" is a newly submitted
talk; 999999 is no talk. A blank Score cell sends a body without "score".


| Result | Scenario | Talk | Reviewer | Score | Status? | Message? |
| --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | Reviewer zero | submitted | 0 | 4 | 400 | reviewerId must be positive |
| ✅ passes | Decimal number | submitted | 1 | 4.0 | 400 | score must be a whole number |
| ✅ passes | No score | submitted | 1 |  | 400 | score must be a whole number |
| ✅ passes | Above the scale | submitted | 1 | 6 | 400 | score must be 1-5 |
| ✅ passes | Bad score for no talk | 999999 | 1 | 6 | 400 | score must be 1-5 |
| ✅ passes | Good score for no talk | 999999 | 1 | 4 | 404 | talk not found |
