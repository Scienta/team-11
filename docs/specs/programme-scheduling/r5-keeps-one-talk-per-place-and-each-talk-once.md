## R5: Keeps one talk per place, and each talk once

A and B are accepted presentations of two different speakers. The programme before is set up
with PUT. The requests of a row are sent one after the other, and the entry is the one in the
answer to the last request.


| Result | Scenario | Programme Before | Requests | Statuses? | Entry? | Programme After? |
| --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | Place has another talk | [R1/S1: B] | [PUT A R1/S1] | [409] |  | [R1/S1: B] |
| ✅ passes | Talk in another room, same slot | [R1/S1: A] | [PUT A R2/S1] | [409] |  | [R1/S1: A] |
| ✅ passes | Talk in another slot | [R1/S1: A] | [PUT A R1/S2] | [409] |  | [R1/S1: A] |
| ✅ passes | Talk taken out first | [R1/S1: A] | [DELETE R1/S1, PUT A R1/S2] | [204, 200] | [R1/S2: A] | [R1/S2: A] |
