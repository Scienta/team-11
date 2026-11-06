## R4: Shows the co-speakers in the answers of the acceptance endpoints

Ada is added to Main's talk, which the committee then waitlists, or Main withdraws.
:talk is the talk's id and :main is Main's speaker id.


| Result | Scenario | Method | Path | Body | Co-speakers? |
| --- | --- | --- | --- | --- | --- |
| ✅ passes | Committee decision | PUT | /talks/:talk/status | {"status":"WAITLISTED"} | [Ada] |
| ✅ passes | Main withdraws | POST | /talks/:talk/withdraw | {"speakerId"::main} | [Ada] |
