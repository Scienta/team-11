## R5: Shows the current status wherever the talk is shown

One speaker has one talk, rated by two different reviewers before it gets its status. In a
place, {talk} is the talk's id and {speaker} is the speaker's id. In a list, the test reads
the status of this talk.


| Result | Scenario | Status | Place | Status Shown? |
| --- | --- | --- | --- | --- |
| ✅ passes | Submitted talk | SUBMITTED | {/talks/{talk}, /talks, /talks?speakerId={speaker}, /talks?sort=rating, /speakers/{speaker}/talks} | SUBMITTED |
| ✅ passes | Accepted talk | ACCEPTED | {/talks/{talk}, /talks, /talks?speakerId={speaker}, /talks?sort=rating, /speakers/{speaker}/talks} | ACCEPTED |
| ✅ passes | Rejected talk | REJECTED | {/talks/{talk}, /talks, /talks?speakerId={speaker}, /talks?sort=rating, /speakers/{speaker}/talks} | REJECTED |
| ✅ passes | Waitlisted talk | WAITLISTED | {/talks/{talk}, /talks, /talks?speakerId={speaker}, /talks?sort=rating, /speakers/{speaker}/talks} | WAITLISTED |
| ✅ passes | Withdrawn talk | WITHDRAWN | {/talks/{talk}, /talks, /talks?speakerId={speaker}, /talks?sort=rating, /speakers/{speaker}/talks} | WITHDRAWN |
