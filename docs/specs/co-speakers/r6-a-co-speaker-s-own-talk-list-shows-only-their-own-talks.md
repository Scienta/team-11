## R6: A co-speaker's own talk list shows only their own talks

Ada co-speaks on Main's talk and has submitted no talk herself. :ada is Ada's speaker id.


| Result | Scenario | Path | Talks? |
| --- | --- | --- | --- |
| ✅ passes | The speaker's talk list | /speakers/:ada/talks | 0 |
| ✅ passes | The talks filter | /talks?speakerId=:ada | 0 |
