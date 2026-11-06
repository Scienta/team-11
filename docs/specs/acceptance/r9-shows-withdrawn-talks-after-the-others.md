## R9: Shows withdrawn talks after the others

Talks A, B and C are submitted in this order. Each score comes from a different reviewer. An
empty list means that nobody has rated the talk. The speaker withdraws the talks in the
Withdrawn column after the ratings.


| Result | Scenario | Talk A | Talk B | Talk C | Withdrawn | List | Titles? |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ✅ passes | Oldest first | [5] | [3] | [4] | [A] | {/talks, /talks?includeWithdrawn=true} | [B, C, A] |
| ✅ passes | One speaker's talks | [5] | [3] | [4] | [A] | /talks?speakerId=1 | [B, C, A] |
| ✅ passes | Ranking, best talk withdrawn | [5] | [3] | [4] | [A] | /talks?sort=rating | [C, B, A] |
| ✅ passes | Ranking with an unrated talk | [5] | [] | [4] | [A] | /talks?sort=rating | [C, B, A] |
| ✅ passes | Ranking, two talks withdrawn | [5] | [3] | [4] | [A, B] | /talks?sort=rating | [C, A, B] |
| ✅ passes | Oldest first, withdrawn left out | [5] | [3] | [4] | [A] | /talks?includeWithdrawn=false | [B, C] |
| ✅ passes | Ranking, withdrawn left out | [5] | [3] | [4] | [A] | /talks?sort=rating&includeWithdrawn=false | [C, B] |
