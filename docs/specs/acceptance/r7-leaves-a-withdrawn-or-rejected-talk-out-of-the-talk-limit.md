## R7: Leaves a withdrawn or rejected talk out of the talk limit

The speaker has three talks, and the third has the status in the table. Then the speaker
submits another talk. The change request submission-cap-cr overrides the earlier rule that
a rejected talk counts.


| Result | Scenario | Third Talk | Response? |
| --- | --- | --- | --- |
| ✅ passes | Third talk still counts | {SUBMITTED, ACCEPTED, WAITLISTED} | 409 |
| ✅ passes | Third talk out of the running | {WITHDRAWN, REJECTED} | 201 |
