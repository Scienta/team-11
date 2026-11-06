# Programme scheduling: our black-box examples (workflow/specs/scheduling/spec.md).
# Changes the shared programme: run only against a local build, never against the team URL.
# Run: hurl --test --variable base_url=http://localhost:8080 examples/local/our-scheduling.hurl
# examples/run.sh does not run it, because run.sh takes only the files directly in examples/.
# Each group registers its own speakers and submits its own talks. A talk is ACCEPTED after ratings
# from two reviewers and the committee's decision (Acceptance R2, R3). The programme has only 15
# places, shared by everyone who uses the service, so each group first empties the places it uses
# and takes its talks out again at the end.
# Hurl sends the requests one at a time, so the R8 rows ("at the same time") are not here.

# ================================================================ R1: the fixed venue

POST {{base_url}}/speakers
{"name": "Edsger Dijkstra", "email": "dijkstra-{{newUuid}}@example.com"}
HTTP 201
[Captures]
venue_speaker: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Shortest paths", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{venue_speaker}}}
HTTP 201
[Captures]
venue_talk: jsonpath "$.id"

PUT {{base_url}}/talks/{{venue_talk}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{venue_talk}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{venue_talk}}/status
{"status": "ACCEPTED"}
HTTP 200

# Setup: empty the places this group uses (204 if something was there, 404 if not)
DELETE {{base_url}}/programme/R1/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S3
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

# R1, R2: R1 S1 is 09:00 to 10:00
PUT {{base_url}}/programme/R1/S1
{"talkId": {{venue_talk}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R1"
jsonpath "$.slot" == "S1"
jsonpath "$.start" == "09:00"
jsonpath "$.end" == "10:00"
jsonpath "$.talkId" == {{venue_talk}}

DELETE {{base_url}}/programme/R1/S1
HTTP 204
[Asserts]
bytes count == 0

# R1, R2: R2 S2 is 10:15 to 11:15
PUT {{base_url}}/programme/R2/S2
{"talkId": {{venue_talk}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R2"
jsonpath "$.slot" == "S2"
jsonpath "$.start" == "10:15"
jsonpath "$.end" == "11:15"
jsonpath "$.talkId" == {{venue_talk}}

DELETE {{base_url}}/programme/R2/S2
HTTP 204
[Asserts]
bytes count == 0

# R1, R2: R3 S3 is 11:30 to 12:30
PUT {{base_url}}/programme/R3/S3
{"talkId": {{venue_talk}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R3"
jsonpath "$.slot" == "S3"
jsonpath "$.start" == "11:30"
jsonpath "$.end" == "12:30"
jsonpath "$.talkId" == {{venue_talk}}

DELETE {{base_url}}/programme/R3/S3
HTTP 204
[Asserts]
bytes count == 0

# R1, R2: R1 S4 is 13:30 to 15:30
PUT {{base_url}}/programme/R1/S4
{"talkId": {{venue_talk}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R1"
jsonpath "$.slot" == "S4"
jsonpath "$.start" == "13:30"
jsonpath "$.end" == "15:30"
jsonpath "$.talkId" == {{venue_talk}}

DELETE {{base_url}}/programme/R1/S4
HTTP 204
[Asserts]
bytes count == 0

# R1, R2: R2 S5 is 15:45 to 16:45
PUT {{base_url}}/programme/R2/S5
{"talkId": {{venue_talk}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R2"
jsonpath "$.slot" == "S5"
jsonpath "$.start" == "15:45"
jsonpath "$.end" == "16:45"
jsonpath "$.talkId" == {{venue_talk}}

DELETE {{base_url}}/programme/R2/S5
HTTP 204
[Asserts]
bytes count == 0

# R1: a place outside the venue does not exist: room R0, as the harness sent at tick 163
PUT {{base_url}}/programme/R0/S2
{"talkId": {{venue_talk}}}
HTTP 404

GET {{base_url}}/programme/R0/S2
HTTP 404

DELETE {{base_url}}/programme/R0/S2
HTTP 404

# R1: a place outside the venue does not exist: slot S0, as the harness sent at tick 164
PUT {{base_url}}/programme/R3/S0
{"talkId": {{venue_talk}}}
HTTP 404

GET {{base_url}}/programme/R3/S0
HTTP 404

DELETE {{base_url}}/programme/R3/S0
HTTP 404

# R1: a place outside the venue does not exist: slot "slot2", as the harness sent at tick 167
PUT {{base_url}}/programme/R2/slot2
{"talkId": {{venue_talk}}}
HTTP 404

GET {{base_url}}/programme/R2/slot2
HTTP 404

DELETE {{base_url}}/programme/R2/slot2
HTTP 404

# R1: a room that is not in the list does not exist
PUT {{base_url}}/programme/R4/S1
{"talkId": {{venue_talk}}}
HTTP 404

GET {{base_url}}/programme/R4/S1
HTTP 404

DELETE {{base_url}}/programme/R4/S1
HTTP 404

# R1: a slot that is not in the list does not exist
PUT {{base_url}}/programme/R1/S6
{"talkId": {{venue_talk}}}
HTTP 404

GET {{base_url}}/programme/R1/S6
HTTP 404

DELETE {{base_url}}/programme/R1/S6
HTTP 404

# R1: a room in lower case
PUT {{base_url}}/programme/r1/S1
{"talkId": {{venue_talk}}}
HTTP 404

GET {{base_url}}/programme/r1/S1
HTTP 404

DELETE {{base_url}}/programme/r1/S1
HTTP 404

# R1: a slot in lower case
PUT {{base_url}}/programme/R1/s1
{"talkId": {{venue_talk}}}
HTTP 404

GET {{base_url}}/programme/R1/s1
HTTP 404

DELETE {{base_url}}/programme/R1/s1
HTTP 404

# ================================================================ R2: put, look up and take out

POST {{base_url}}/speakers
{"name": "Tony Hoare", "email": "hoare-{{newUuid}}@example.com"}
HTTP 201
[Captures]
hoare: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Quicksort on a whiteboard", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{hoare}}}
HTTP 201
[Captures]
quicksort: jsonpath "$.id"

PUT {{base_url}}/talks/{{quicksort}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{quicksort}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{quicksort}}/status
{"status": "ACCEPTED"}
HTTP 200

# Setup: empty the places this group uses (204 if something was there, 404 if not)
DELETE {{base_url}}/programme/R1/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S3
HTTP *
[Asserts]
status toString matches /^(204|404)$/

# R2: put the talk in R1 S1, and the response is the entry
PUT {{base_url}}/programme/R1/S1
{"talkId": {{quicksort}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R1"
jsonpath "$.slot" == "S1"
jsonpath "$.start" == "09:00"
jsonpath "$.end" == "10:00"
jsonpath "$.talkId" == {{quicksort}}

# R2: look up R1 S1
GET {{base_url}}/programme/R1/S1
HTTP 200
[Asserts]
jsonpath "$.room" == "R1"
jsonpath "$.slot" == "S1"
jsonpath "$.start" == "09:00"
jsonpath "$.end" == "10:00"
jsonpath "$.talkId" == {{quicksort}}

# Our decisions: the same talk in the same place again changes nothing
PUT {{base_url}}/programme/R1/S1
{"talkId": {{quicksort}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R1"
jsonpath "$.slot" == "S1"
jsonpath "$.start" == "09:00"
jsonpath "$.end" == "10:00"
jsonpath "$.talkId" == {{quicksort}}

# R2: an empty place
GET {{base_url}}/programme/R1/S2
HTTP 404

# R2: take the talk out, and the place is empty
DELETE {{base_url}}/programme/R1/S1
HTTP 204
[Asserts]
bytes count == 0

GET {{base_url}}/programme/R1/S1
HTTP 404

# R2: take out from an empty place
DELETE {{base_url}}/programme/R1/S1
HTTP 404

# R2: the talk that was taken out goes somewhere else
PUT {{base_url}}/programme/R2/S3
{"talkId": {{quicksort}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R2"
jsonpath "$.slot" == "S3"
jsonpath "$.start" == "11:30"
jsonpath "$.end" == "12:30"
jsonpath "$.talkId" == {{quicksort}}

DELETE {{base_url}}/programme/R2/S3
HTTP 204
[Asserts]
bytes count == 0

# ================================================================ R2: the PUT body

# Setup: empty the places this group uses (204 if something was there, 404 if not)
DELETE {{base_url}}/programme/R3/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

# Our decisions: no body
PUT {{base_url}}/programme/R3/S1
Content-Type: application/json
HTTP 400

# Our decisions: not JSON
PUT {{base_url}}/programme/R3/S1
Content-Type: application/json
```
{"talkId":
```
HTTP 400

# Our decisions: no talkId
PUT {{base_url}}/programme/R3/S1
{}
HTTP 400

# Our decisions: talkId null
PUT {{base_url}}/programme/R3/S1
{"talkId": null}
HTTP 400

# Our decisions: talkId as a string
PUT {{base_url}}/programme/R3/S1
{"talkId": "{{quicksort}}"}
HTTP 400

# Our decisions: talkId written as a decimal number
PUT {{base_url}}/programme/R3/S1
Content-Type: application/json
```
{"talkId": {{quicksort}}.0}
```
HTTP 400

# Our decisions: no talk with this ID
PUT {{base_url}}/programme/R3/S1
{"talkId": 999999999}
HTTP 400

# Our decisions: talk ID 0
PUT {{base_url}}/programme/R3/S1
{"talkId": 0}
HTTP 400

# Our decisions: none of the bad requests saved an entry
GET {{base_url}}/programme/R3/S1
HTTP 404

# ================================================================ R3: only an accepted talk

POST {{base_url}}/speakers
{"name": "Barbara Liskov", "email": "liskov-{{newUuid}}@example.com"}
HTTP 201
[Captures]
liskov: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Data abstraction", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{liskov}}}
HTTP 201
[Captures]
submitted: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Substitution", "format": "LIGHTNING_20", "level": "INTERMEDIATE", "speakerId": {{liskov}}}
HTTP 201
[Captures]
waitlisted: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "CLU revisited", "format": "LIGHTNING_10", "level": "INTERMEDIATE", "speakerId": {{liskov}}}
HTTP 201
[Captures]
rejected: jsonpath "$.id"

PUT {{base_url}}/talks/{{waitlisted}}/status
{"status": "WAITLISTED"}
HTTP 200

PUT {{base_url}}/talks/{{rejected}}/status
{"status": "REJECTED"}
HTTP 200

POST {{base_url}}/speakers
{"name": "Jeannette Wing", "email": "wing-{{newUuid}}@example.com"}
HTTP 201
[Captures]
wing: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Computational thinking", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{wing}}}
HTTP 201
[Captures]
withdrawn: jsonpath "$.id"

PUT {{base_url}}/talks/{{withdrawn}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{withdrawn}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{withdrawn}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks/{{withdrawn}}/withdraw
{"speakerId": {{wing}}}
HTTP 200

# Setup: empty the places this group uses (204 if something was there, 404 if not)
DELETE {{base_url}}/programme/R3/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

# R3: a SUBMITTED talk
PUT {{base_url}}/programme/R3/S2
{"talkId": {{submitted}}}
HTTP 409

# R3: a WAITLISTED talk
PUT {{base_url}}/programme/R3/S2
{"talkId": {{waitlisted}}}
HTTP 409

# R3: a REJECTED talk
PUT {{base_url}}/programme/R3/S2
{"talkId": {{rejected}}}
HTTP 409

# R3: a WITHDRAWN talk
PUT {{base_url}}/programme/R3/S2
{"talkId": {{withdrawn}}}
HTTP 409

# R3: none of the talks went on the programme
GET {{base_url}}/programme/R3/S2
HTTP 404

# ================================================================ R4: a workshop needs the long slot

POST {{base_url}}/speakers
{"name": "Alan Kay", "email": "kay-{{newUuid}}@example.com"}
HTTP 201
[Captures]
kay: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Build a Dynabook", "format": "WORKSHOP", "level": "INTERMEDIATE", "speakerId": {{kay}}}
HTTP 201
[Captures]
workshop: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Objects in ten minutes", "format": "LIGHTNING_10", "level": "INTERMEDIATE", "speakerId": {{kay}}}
HTTP 201
[Captures]
lightning_10: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Messages, not methods", "format": "LIGHTNING_20", "level": "INTERMEDIATE", "speakerId": {{kay}}}
HTTP 201
[Captures]
lightning_20: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Adele Goldberg", "email": "goldberg-{{newUuid}}@example.com"}
HTTP 201
[Captures]
goldberg: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Smalltalk-80", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{goldberg}}}
HTTP 201
[Captures]
presentation: jsonpath "$.id"

PUT {{base_url}}/talks/{{workshop}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{workshop}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{workshop}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{lightning_10}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{lightning_10}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{lightning_10}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{lightning_20}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{lightning_20}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{lightning_20}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{presentation}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{presentation}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{presentation}}/status
{"status": "ACCEPTED"}
HTTP 200

# Setup: empty the places this group uses (204 if something was there, 404 if not)
DELETE {{base_url}}/programme/R2/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S3
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

# R4: a workshop in S1
PUT {{base_url}}/programme/R2/S1
{"talkId": {{workshop}}}
HTTP 409

# R4: a workshop in S2
PUT {{base_url}}/programme/R2/S2
{"talkId": {{workshop}}}
HTTP 409

# R4: a workshop in S3
PUT {{base_url}}/programme/R2/S3
{"talkId": {{workshop}}}
HTTP 409

# R4: a workshop in S5
PUT {{base_url}}/programme/R2/S5
{"talkId": {{workshop}}}
HTTP 409

# R4: a workshop in the long slot S4
PUT {{base_url}}/programme/R2/S4
{"talkId": {{workshop}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R2"
jsonpath "$.slot" == "S4"
jsonpath "$.start" == "13:30"
jsonpath "$.end" == "15:30"
jsonpath "$.talkId" == {{workshop}}

DELETE {{base_url}}/programme/R2/S4
HTTP 204
[Asserts]
bytes count == 0

# R4: a LIGHTNING_10 in S4
PUT {{base_url}}/programme/R2/S4
{"talkId": {{lightning_10}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R2"
jsonpath "$.slot" == "S4"
jsonpath "$.start" == "13:30"
jsonpath "$.end" == "15:30"
jsonpath "$.talkId" == {{lightning_10}}

DELETE {{base_url}}/programme/R2/S4
HTTP 204
[Asserts]
bytes count == 0

# R4: a LIGHTNING_10 in S1
PUT {{base_url}}/programme/R2/S1
{"talkId": {{lightning_10}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R2"
jsonpath "$.slot" == "S1"
jsonpath "$.start" == "09:00"
jsonpath "$.end" == "10:00"
jsonpath "$.talkId" == {{lightning_10}}

DELETE {{base_url}}/programme/R2/S1
HTTP 204
[Asserts]
bytes count == 0

# R4: a LIGHTNING_20 in S4
PUT {{base_url}}/programme/R2/S4
{"talkId": {{lightning_20}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R2"
jsonpath "$.slot" == "S4"
jsonpath "$.start" == "13:30"
jsonpath "$.end" == "15:30"
jsonpath "$.talkId" == {{lightning_20}}

DELETE {{base_url}}/programme/R2/S4
HTTP 204
[Asserts]
bytes count == 0

# R4: a LIGHTNING_20 in S1
PUT {{base_url}}/programme/R2/S1
{"talkId": {{lightning_20}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R2"
jsonpath "$.slot" == "S1"
jsonpath "$.start" == "09:00"
jsonpath "$.end" == "10:00"
jsonpath "$.talkId" == {{lightning_20}}

DELETE {{base_url}}/programme/R2/S1
HTTP 204
[Asserts]
bytes count == 0

# R4: a PRESENTATION in S4
PUT {{base_url}}/programme/R2/S4
{"talkId": {{presentation}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R2"
jsonpath "$.slot" == "S4"
jsonpath "$.start" == "13:30"
jsonpath "$.end" == "15:30"
jsonpath "$.talkId" == {{presentation}}

DELETE {{base_url}}/programme/R2/S4
HTTP 204
[Asserts]
bytes count == 0

# R4: a PRESENTATION in S1
PUT {{base_url}}/programme/R2/S1
{"talkId": {{presentation}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R2"
jsonpath "$.slot" == "S1"
jsonpath "$.start" == "09:00"
jsonpath "$.end" == "10:00"
jsonpath "$.talkId" == {{presentation}}

DELETE {{base_url}}/programme/R2/S1
HTTP 204
[Asserts]
bytes count == 0

# ================================================================ R5: one talk per room and slot, and once on the programme

POST {{base_url}}/speakers
{"name": "Donald Knuth", "email": "knuth-{{newUuid}}@example.com"}
HTTP 201
[Captures]
knuth: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Literate programming", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{knuth}}}
HTTP 201
[Captures]
talk_a: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Leslie Lamport", "email": "lamport-{{newUuid}}@example.com"}
HTTP 201
[Captures]
lamport: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Time, clocks and ordering", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{lamport}}}
HTTP 201
[Captures]
talk_b: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk_a}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk_a}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk_a}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{talk_b}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk_b}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk_b}}/status
{"status": "ACCEPTED"}
HTTP 200

# Setup: empty the places this group uses (204 if something was there, 404 if not)
DELETE {{base_url}}/programme/R3/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S3
HTTP *
[Asserts]
status toString matches /^(204|404)$/

PUT {{base_url}}/programme/R3/S5
{"talkId": {{talk_b}}}
HTTP 200

# R5: another talk is in R3 S5
PUT {{base_url}}/programme/R3/S5
{"talkId": {{talk_a}}}
HTTP 409

GET {{base_url}}/programme/R3/S5
HTTP 200
[Asserts]
jsonpath "$.talkId" == {{talk_b}}

PUT {{base_url}}/programme/R2/S5
{"talkId": {{talk_a}}}
HTTP 200

# R5: the talk is already in another room in the same slot
PUT {{base_url}}/programme/R1/S5
{"talkId": {{talk_a}}}
HTTP 409

GET {{base_url}}/programme/R1/S5
HTTP 404

# R5: the talk is already in another slot
PUT {{base_url}}/programme/R2/S3
{"talkId": {{talk_a}}}
HTTP 409

GET {{base_url}}/programme/R2/S3
HTTP 404

DELETE {{base_url}}/programme/R2/S5
HTTP 204
[Asserts]
bytes count == 0

# R5: to move the talk, take it out first
PUT {{base_url}}/programme/R2/S3
{"talkId": {{talk_a}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R2"
jsonpath "$.slot" == "S3"
jsonpath "$.start" == "11:30"
jsonpath "$.end" == "12:30"
jsonpath "$.talkId" == {{talk_a}}

GET {{base_url}}/programme/R2/S5
HTTP 404

DELETE {{base_url}}/programme/R2/S3
HTTP 204
[Asserts]
bytes count == 0

DELETE {{base_url}}/programme/R3/S5
HTTP 204
[Asserts]
bytes count == 0

# ================================================================ R6: nobody speaks in two rooms at the same time

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Charles Babbage", "email": "babbage-{{newUuid}}@example.com"}
HTTP 201
[Captures]
bob: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Mary Somerville", "email": "somerville-{{newUuid}}@example.com"}
HTTP 201
[Captures]
cy: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Notes on the engine", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{ada}}}
HTTP 201
[Captures]
ada_first: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Bernoulli numbers", "format": "LIGHTNING_20", "level": "INTERMEDIATE", "speakerId": {{ada}}}
HTTP 201
[Captures]
ada_second: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "The analytical engine", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{bob}}}
HTTP 201
[Captures]
bob_with_ada: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "The difference engine", "format": "LIGHTNING_10", "level": "INTERMEDIATE", "speakerId": {{bob}}}
HTTP 201
[Captures]
bob_alone: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "The connexion of the sciences", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{cy}}}
HTTP 201
[Captures]
cy_with_ada: jsonpath "$.id"

PUT {{base_url}}/talks/{{bob_with_ada}}/co-speakers/{{ada}}
HTTP 200

PUT {{base_url}}/talks/{{ada_first}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{ada_first}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{ada_first}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{ada_second}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{ada_second}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{ada_second}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{bob_with_ada}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{bob_with_ada}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{bob_with_ada}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{bob_alone}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{bob_alone}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{bob_alone}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{cy_with_ada}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{cy_with_ada}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{cy_with_ada}}/status
{"status": "ACCEPTED"}
HTTP 200

# Setup: empty the places this group uses (204 if something was there, 404 if not)
DELETE {{base_url}}/programme/R1/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S3
HTTP *
[Asserts]
status toString matches /^(204|404)$/

PUT {{base_url}}/programme/R1/S2
{"talkId": {{ada_first}}}
HTTP 200

# R6: Ada is the main speaker of a talk in R1 S2
PUT {{base_url}}/programme/R2/S2
{"talkId": {{ada_second}}}
HTTP 409

# R6: Ada, the co-speaker, is the main speaker of a talk in R1 S2
PUT {{base_url}}/programme/R2/S2
{"talkId": {{bob_with_ada}}}
HTTP 409

# R6: Bob is not in R1 S2
PUT {{base_url}}/programme/R2/S2
{"talkId": {{bob_alone}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R2"
jsonpath "$.slot" == "S2"
jsonpath "$.start" == "10:15"
jsonpath "$.end" == "11:15"
jsonpath "$.talkId" == {{bob_alone}}

# R6: another talk of Ada in another slot
PUT {{base_url}}/programme/R2/S3
{"talkId": {{ada_second}}}
HTTP 200
[Asserts]
jsonpath "$.room" == "R2"
jsonpath "$.slot" == "S3"
jsonpath "$.start" == "11:30"
jsonpath "$.end" == "12:30"
jsonpath "$.talkId" == {{ada_second}}

DELETE {{base_url}}/programme/R2/S2
HTTP 204
[Asserts]
bytes count == 0

DELETE {{base_url}}/programme/R2/S3
HTTP 204
[Asserts]
bytes count == 0

DELETE {{base_url}}/programme/R1/S2
HTTP 204
[Asserts]
bytes count == 0

PUT {{base_url}}/programme/R1/S2
{"talkId": {{bob_with_ada}}}
HTTP 200

# R6: Ada is a co-speaker of a talk in R1 S2
PUT {{base_url}}/programme/R2/S2
{"talkId": {{ada_second}}}
HTTP 409

# Setup: Ada withdraws her second talk, so she can co-speak on a third talk (the limit of 3, submission-cap-cr)
POST {{base_url}}/talks/{{ada_second}}/withdraw
{"speakerId": {{ada}}}
HTTP 200

PUT {{base_url}}/talks/{{cy_with_ada}}/co-speakers/{{ada}}
HTTP 200

# R6: Ada is a co-speaker of a talk in R1 S2 and of this talk
PUT {{base_url}}/programme/R2/S2
{"talkId": {{cy_with_ada}}}
HTTP 409

GET {{base_url}}/programme/R2/S2
HTTP 404

DELETE {{base_url}}/programme/R1/S2
HTTP 204
[Asserts]
bytes count == 0

# ================================================================ R7: later changes leave the programme as it is

POST {{base_url}}/speakers
{"name": "Grace Hopper", "email": "hopper-{{newUuid}}@example.com"}
HTTP 201
[Captures]
hopper: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Nanoseconds", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{hopper}}}
HTTP 201
[Captures]
to_withdraw: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Frances Allen", "email": "allen-{{newUuid}}@example.com"}
HTTP 201
[Captures]
allen: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "John Backus", "email": "backus-{{newUuid}}@example.com"}
HTTP 201
[Captures]
backus: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Program optimisation", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{allen}}}
HTTP 201
[Captures]
allen_talk: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Can programming be liberated?", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{backus}}}
HTTP 201
[Captures]
backus_talk: jsonpath "$.id"

PUT {{base_url}}/talks/{{to_withdraw}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{to_withdraw}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{to_withdraw}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{allen_talk}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{allen_talk}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{allen_talk}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{backus_talk}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{backus_talk}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{backus_talk}}/status
{"status": "ACCEPTED"}
HTTP 200

# Setup: empty the places this group uses (204 if something was there, 404 if not)
DELETE {{base_url}}/programme/R3/S3
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

PUT {{base_url}}/programme/R3/S3
{"talkId": {{to_withdraw}}}
HTTP 200

POST {{base_url}}/talks/{{to_withdraw}}/withdraw
{"speakerId": {{hopper}}}
HTTP 200

# R7: the withdrawn talk stays on the programme
GET {{base_url}}/programme/R3/S3
HTTP 200
[Asserts]
jsonpath "$.talkId" == {{to_withdraw}}

# Our decisions: the committee takes the withdrawn talk out
DELETE {{base_url}}/programme/R3/S3
HTTP 204
[Asserts]
bytes count == 0

PUT {{base_url}}/programme/R1/S1
{"talkId": {{allen_talk}}}
HTTP 200

PUT {{base_url}}/programme/R2/S1
{"talkId": {{backus_talk}}}
HTTP 200

PUT {{base_url}}/talks/{{backus_talk}}/co-speakers/{{allen}}
HTTP 200

# R7: Allen is now in both talks of S1, and both entries stay
GET {{base_url}}/programme/R1/S1
HTTP 200
[Asserts]
jsonpath "$.talkId" == {{allen_talk}}

GET {{base_url}}/programme/R2/S1
HTTP 200
[Asserts]
jsonpath "$.talkId" == {{backus_talk}}

DELETE {{base_url}}/programme/R1/S1
HTTP 204
[Asserts]
bytes count == 0

DELETE {{base_url}}/programme/R2/S1
HTTP 204
[Asserts]
bytes count == 0
