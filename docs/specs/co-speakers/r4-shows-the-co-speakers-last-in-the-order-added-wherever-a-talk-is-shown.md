## R4: Shows the co-speakers last, in the order added, wherever a talk is shown

Bob and then Ada are added to Main's talk; Main's second talk has no co-speakers.
:talk is the first talk's id and :main is Main's speaker id.


| Scenario | Path | Co-speakers? | Without co-speakers? |
| --- | --- | --- | --- |
| One talk | /talks/:talk | [Bob, Ada] | [] |
| All talks | /talks | [Bob, Ada] | [] |
| One speaker's talks | /talks?speakerId=:main | [Bob, Ada] | [] |
| The ranking | /talks?sort=rating | [Bob, Ada] | [] |
| One speaker's ranking | /talks?speakerId=:main&sort=rating | [Bob, Ada] | [] |
| The speaker's own talk list | /speakers/:main/talks | [Bob, Ada] | [] |
