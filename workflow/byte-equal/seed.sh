#!/usr/bin/env bash
# Seeds talks in every status, with ratings and a co-speaker, through the API.
# Usage: workflow/byte-equal/seed.sh <base-url>     (run it once, on an empty database)
set -euo pipefail
b="${1:?base url, for example http://localhost:8091}"
j() { curl -fsS -X "$1" -H 'Content-Type: application/json' "$b$2" ${3:+-d "$3"} >/dev/null; }
for n in 1 2 3; do j POST /speakers "{\"name\":\"Speaker $n\",\"email\":\"s$n@example.com\"}"; done
talk() { j POST /talks "{\"title\":\"$1\",\"format\":\"$2\",\"level\":\"BEGINNER\",\"speakerId\":$3}"; }
talk "Accepted one" PRESENTATION 1; talk "Waitlisted" LIGHTNING_10 1; talk "Withdrawn best" PRESENTATION 1
talk "Three fours" WORKSHOP 2; talk "Rejected" LIGHTNING_20 2; talk "Unrated" PRESENTATION 2
talk "Workshop with co" WORKSHOP 3
rate() { j PUT "/talks/$1/ratings/$2" "{\"score\":$3}"; }
rate 1 1 5; rate 1 2 4; rate 2 1 3; rate 3 1 5; rate 3 2 5; rate 4 1 4; rate 4 2 4; rate 4 3 4; rate 7 1 2; rate 7 2 3
j PUT /talks/1/status '{"status":"ACCEPTED"}'; j PUT /talks/2/status '{"status":"WAITLISTED"}'
j PUT /talks/5/status '{"status":"REJECTED"}'; j PUT /talks/7/status '{"status":"ACCEPTED"}'
j POST /talks/3/withdraw '{"speakerId":1}'
j PUT /talks/7/co-speakers/1
