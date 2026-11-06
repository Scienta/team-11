# Acceptance state machine: our black-box examples (workflow/specs/acceptance/spec.md).
# Run: examples/run.sh [base-url]
# Each group registers its own speakers with unique emails, so the file runs against a shared
# database. Lists filter on speakerId for the same reason. A speaker has at most 3 talks.
# Every talk in the decision groups has ratings from two reviewers before it gets a status,
# except in the R3 group.
# Hurl sends the requests one at a time, so R6 (two committee members at once) is not here.

# ================================================================ R1, R2: the committee's decision

POST {{base_url}}/speakers
{"name": "Grace Hopper", "email": "grace-{{newUuid}}@example.com"}
HTTP 201
[Captures]
hopper: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Compilers for business", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{hopper}}}
HTTP 201
[Captures]
to_accept: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Nanoseconds explained", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{hopper}}}
HTTP 201
[Captures]
to_reject: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Debugging the Mark II", "format": "WORKSHOP", "level": "ADVANCED", "speakerId": {{hopper}}}
HTTP 201
[Captures]
to_waitlist: jsonpath "$.id"

PUT {{base_url}}/talks/{{to_accept}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{to_accept}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{to_reject}}/ratings/1
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{to_reject}}/ratings/2
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{to_waitlist}}/ratings/1
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{to_waitlist}}/ratings/2
{"score": 3}
HTTP 200

# R2, R5: SUBMITTED -> ACCEPTED, and the response is the talk
PUT {{base_url}}/talks/{{to_accept}}/status
{"status": "ACCEPTED"}
HTTP 200
[Asserts]
jsonpath "$.id" == {{to_accept}}
jsonpath "$.title" == "Compilers for business"
jsonpath "$.speakerId" == {{hopper}}
jsonpath "$.durationMinutes" == 45
jsonpath "$.status" == "ACCEPTED"
jsonpath "$.averageRating" == 4.5
jsonpath "$.ratingCount" == 2

# R2: SUBMITTED -> REJECTED
PUT {{base_url}}/talks/{{to_reject}}/status
{"status": "REJECTED"}
HTTP 200
[Asserts]
jsonpath "$.id" == {{to_reject}}
jsonpath "$.status" == "REJECTED"

# R2: SUBMITTED -> WAITLISTED
PUT {{base_url}}/talks/{{to_waitlist}}/status
{"status": "WAITLISTED"}
HTTP 200
[Asserts]
jsonpath "$.id" == {{to_waitlist}}
jsonpath "$.status" == "WAITLISTED"

# R2, R6: WAITLISTED -> WAITLISTED is 409, and the talk stays WAITLISTED
PUT {{base_url}}/talks/{{to_waitlist}}/status
{"status": "WAITLISTED"}
HTTP 409

GET {{base_url}}/talks/{{to_waitlist}}
HTTP 200
[Asserts]
jsonpath "$.status" == "WAITLISTED"

# R2, R6: an ACCEPTED talk gets no decision, and stays ACCEPTED
PUT {{base_url}}/talks/{{to_accept}}/status
{"status": "ACCEPTED"}
HTTP 409

PUT {{base_url}}/talks/{{to_accept}}/status
{"status": "REJECTED"}
HTTP 409

PUT {{base_url}}/talks/{{to_accept}}/status
{"status": "WAITLISTED"}
HTTP 409

GET {{base_url}}/talks/{{to_accept}}
HTTP 200
[Asserts]
jsonpath "$.status" == "ACCEPTED"

# R2, R6: a REJECTED talk gets no decision, and stays REJECTED
PUT {{base_url}}/talks/{{to_reject}}/status
{"status": "ACCEPTED"}
HTTP 409

PUT {{base_url}}/talks/{{to_reject}}/status
{"status": "REJECTED"}
HTTP 409

PUT {{base_url}}/talks/{{to_reject}}/status
{"status": "WAITLISTED"}
HTTP 409

GET {{base_url}}/talks/{{to_reject}}
HTTP 200
[Asserts]
jsonpath "$.status" == "REJECTED"

# R2: WAITLISTED -> ACCEPTED
PUT {{base_url}}/talks/{{to_waitlist}}/status
{"status": "ACCEPTED"}
HTTP 200
[Asserts]
jsonpath "$.status" == "ACCEPTED"

# R5: the status shows in every list that shows the talk
GET {{base_url}}/talks/{{to_accept}}
HTTP 200
[Asserts]
jsonpath "$.status" == "ACCEPTED"

GET {{base_url}}/speakers/{{hopper}}/talks
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].id" == {{to_accept}}
jsonpath "$[0].status" == "ACCEPTED"
jsonpath "$[1].id" == {{to_reject}}
jsonpath "$[1].status" == "REJECTED"
jsonpath "$[2].id" == {{to_waitlist}}
jsonpath "$[2].status" == "ACCEPTED"

GET {{base_url}}/talks
[Query]
speakerId: {{hopper}}
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].status" == "ACCEPTED"
jsonpath "$[1].status" == "REJECTED"
jsonpath "$[2].status" == "ACCEPTED"

GET {{base_url}}/talks
[Query]
speakerId: {{hopper}}
sort: rating
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].id" == {{to_accept}}
jsonpath "$[0].status" == "ACCEPTED"

POST {{base_url}}/speakers
{"name": "Mary Kenneth Keller", "email": "keller-{{newUuid}}@example.com"}
HTTP 201
[Captures]
keller: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Inductive inference on computers", "format": "PRESENTATION", "level": "ADVANCED", "speakerId": {{keller}}}
HTTP 201
[Captures]
waitlist_then_reject: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Computers in the classroom", "format": "LIGHTNING_20", "level": "BEGINNER", "speakerId": {{keller}}}
HTTP 201
[Captures]
withdrawn_first: jsonpath "$.id"

PUT {{base_url}}/talks/{{waitlist_then_reject}}/ratings/1
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{waitlist_then_reject}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{withdrawn_first}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{withdrawn_first}}/ratings/2
{"score": 5}
HTTP 200

# R2: WAITLISTED -> REJECTED
PUT {{base_url}}/talks/{{waitlist_then_reject}}/status
{"status": "WAITLISTED"}
HTTP 200

PUT {{base_url}}/talks/{{waitlist_then_reject}}/status
{"status": "REJECTED"}
HTTP 200
[Asserts]
jsonpath "$.status" == "REJECTED"

# R4, R6: a WITHDRAWN talk gets no decision, and stays WITHDRAWN
POST {{base_url}}/talks/{{withdrawn_first}}/withdraw
{"speakerId": {{keller}}}
HTTP 200

PUT {{base_url}}/talks/{{withdrawn_first}}/status
{"status": "ACCEPTED"}
HTTP 409

PUT {{base_url}}/talks/{{withdrawn_first}}/status
{"status": "REJECTED"}
HTTP 409

PUT {{base_url}}/talks/{{withdrawn_first}}/status
{"status": "WAITLISTED"}
HTTP 409

GET {{base_url}}/talks/{{withdrawn_first}}
HTTP 200
[Asserts]
jsonpath "$.status" == "WITHDRAWN"

# ================================================================ R3: two different reviewers before acceptance

POST {{base_url}}/speakers
{"name": "Adele Goldberg", "email": "adele-{{newUuid}}@example.com"}
HTTP 201
[Captures]
goldberg: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Smalltalk-80", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{goldberg}}}
HTTP 201
[Captures]
reviewed: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Objects for children", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{goldberg}}}
HTTP 201
[Captures]
unrated_reject: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "The Dynabook", "format": "LIGHTNING_20", "level": "BEGINNER", "speakerId": {{goldberg}}}
HTTP 201
[Captures]
unrated_waitlist: jsonpath "$.id"

# R3: no ratings, ACCEPTED is 409
PUT {{base_url}}/talks/{{reviewed}}/status
{"status": "ACCEPTED"}
HTTP 409

GET {{base_url}}/talks/{{reviewed}}
HTTP 200
[Asserts]
jsonpath "$.status" == "SUBMITTED"

# R3: one reviewer, ACCEPTED is 409
PUT {{base_url}}/talks/{{reviewed}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{reviewed}}/status
{"status": "ACCEPTED"}
HTTP 409

# R3: reviewer 1 rates twice, and is still one reviewer
PUT {{base_url}}/talks/{{reviewed}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{reviewed}}/status
{"status": "ACCEPTED"}
HTTP 409

GET {{base_url}}/talks/{{reviewed}}
HTTP 200
[Asserts]
jsonpath "$.status" == "SUBMITTED"
jsonpath "$.ratingCount" == 1

# R3: reviewers 1 and 2, ACCEPTED is 200
PUT {{base_url}}/talks/{{reviewed}}/ratings/2
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{reviewed}}/status
{"status": "ACCEPTED"}
HTTP 200
[Asserts]
jsonpath "$.status" == "ACCEPTED"
jsonpath "$.ratingCount" == 2

# R3: REJECTED needs no ratings
PUT {{base_url}}/talks/{{unrated_reject}}/status
{"status": "REJECTED"}
HTTP 200
[Asserts]
jsonpath "$.status" == "REJECTED"
jsonpath "$.ratingCount" == 0

# R3: WAITLISTED needs no ratings
PUT {{base_url}}/talks/{{unrated_waitlist}}/status
{"status": "WAITLISTED"}
HTTP 200
[Asserts]
jsonpath "$.status" == "WAITLISTED"
jsonpath "$.ratingCount" == 0

# ================================================================ R1: what a decision is

POST {{base_url}}/speakers
{"name": "Jean Bartik", "email": "bartik-{{newUuid}}@example.com"}
HTTP 201
[Captures]
bartik: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Programming the ENIAC", "format": "WORKSHOP", "level": "ADVANCED", "speakerId": {{bartik}}}
HTTP 201
[Captures]
undecided: jsonpath "$.id"

# R1: SUBMITTED and WITHDRAWN are not decisions
PUT {{base_url}}/talks/{{undecided}}/status
{"status": "SUBMITTED"}
HTTP 400

PUT {{base_url}}/talks/{{undecided}}/status
{"status": "WITHDRAWN"}
HTTP 400

# R1: only the three decisions, in capitals
PUT {{base_url}}/talks/{{undecided}}/status
{"status": "accepted"}
HTTP 400

PUT {{base_url}}/talks/{{undecided}}/status
{"status": "MAYBE"}
HTTP 400

PUT {{base_url}}/talks/{{undecided}}/status
{"status": ""}
HTTP 400

PUT {{base_url}}/talks/{{undecided}}/status
{"status": 1}
HTTP 400

PUT {{base_url}}/talks/{{undecided}}/status
{"status": null}
HTTP 400

PUT {{base_url}}/talks/{{undecided}}/status
{}
HTTP 400

# R1: malformed JSON and no body
PUT {{base_url}}/talks/{{undecided}}/status
Content-Type: application/json
```
{"status": "REJECTED"
```
HTTP 400

PUT {{base_url}}/talks/{{undecided}}/status
Content-Type: application/json
HTTP 400

# R6: none of the bad requests changed the talk
GET {{base_url}}/talks/{{undecided}}
HTTP 200
[Asserts]
jsonpath "$.status" == "SUBMITTED"

# R1: a decision
PUT {{base_url}}/talks/{{undecided}}/status
{"status": "WAITLISTED"}
HTTP 200
[Asserts]
jsonpath "$.status" == "WAITLISTED"

# ================================================================ unknown talk

PUT {{base_url}}/talks/999999999/status
{"status": "REJECTED"}
HTTP 404

POST {{base_url}}/talks/999999999/withdraw
{"speakerId": {{bartik}}}
HTTP 404

# ================================================================ R4: who can withdraw

POST {{base_url}}/speakers
{"name": "Radia Perlman", "email": "radia-{{newUuid}}@example.com"}
HTTP 201
[Captures]
perlman: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Sophie Wilson", "email": "sophie-{{newUuid}}@example.com"}
HTTP 201
[Captures]
wilson: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Spanning trees", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{perlman}}}
HTTP 201
[Captures]
spanning_trees: jsonpath "$.id"

# R4: another registered speaker gets 403
POST {{base_url}}/talks/{{spanning_trees}}/withdraw
{"speakerId": {{wilson}}}
HTTP 403

# R4: an unknown speaker gets 403
POST {{base_url}}/talks/{{spanning_trees}}/withdraw
{"speakerId": 999999999}
HTTP 403

# R4: no speakerId, or a speakerId that is not a JSON integer, gets 400
POST {{base_url}}/talks/{{spanning_trees}}/withdraw
{}
HTTP 400

POST {{base_url}}/talks/{{spanning_trees}}/withdraw
{"speakerId": "{{perlman}}"}
HTTP 400

POST {{base_url}}/talks/{{spanning_trees}}/withdraw
{"speakerId": null}
HTTP 400

POST {{base_url}}/talks/{{spanning_trees}}/withdraw
Content-Type: application/json
HTTP 400

# R6: none of these requests changed the talk
GET {{base_url}}/talks/{{spanning_trees}}
HTTP 200
[Asserts]
jsonpath "$.status" == "SUBMITTED"

# R4, R5: the main speaker withdraws a SUBMITTED talk, and the response is the talk
POST {{base_url}}/talks/{{spanning_trees}}/withdraw
{"speakerId": {{perlman}}}
HTTP 200
[Asserts]
jsonpath "$.id" == {{spanning_trees}}
jsonpath "$.title" == "Spanning trees"
jsonpath "$.speakerId" == {{perlman}}
jsonpath "$.status" == "WITHDRAWN"

GET {{base_url}}/talks/{{spanning_trees}}
HTTP 200
[Asserts]
jsonpath "$.status" == "WITHDRAWN"

# ================================================================ R4, R6: which talks the main speaker can withdraw

POST {{base_url}}/speakers
{"name": "Karen Sparck Jones", "email": "karen-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sparck_jones: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Inverse document frequency", "format": "PRESENTATION", "level": "ADVANCED", "speakerId": {{sparck_jones}}}
HTTP 201
[Captures]
waitlisted: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Term specificity", "format": "LIGHTNING_20", "level": "INTERMEDIATE", "speakerId": {{sparck_jones}}}
HTTP 201
[Captures]
accepted: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Synonymy and semantic classification", "format": "WORKSHOP", "level": "ADVANCED", "speakerId": {{sparck_jones}}}
HTTP 201
[Captures]
rejected: jsonpath "$.id"

PUT {{base_url}}/talks/{{waitlisted}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{waitlisted}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{accepted}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{accepted}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{rejected}}/ratings/1
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{rejected}}/ratings/2
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{waitlisted}}/status
{"status": "WAITLISTED"}
HTTP 200

PUT {{base_url}}/talks/{{accepted}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{rejected}}/status
{"status": "REJECTED"}
HTTP 200

# R4: the main speaker withdraws a WAITLISTED talk
POST {{base_url}}/talks/{{waitlisted}}/withdraw
{"speakerId": {{sparck_jones}}}
HTTP 200
[Asserts]
jsonpath "$.status" == "WITHDRAWN"

# R4: the main speaker withdraws an ACCEPTED talk
POST {{base_url}}/talks/{{accepted}}/withdraw
{"speakerId": {{sparck_jones}}}
HTTP 200
[Asserts]
jsonpath "$.status" == "WITHDRAWN"

# R4, R6: a WITHDRAWN talk cannot be withdrawn again, and stays WITHDRAWN
POST {{base_url}}/talks/{{accepted}}/withdraw
{"speakerId": {{sparck_jones}}}
HTTP 409

GET {{base_url}}/talks/{{accepted}}
HTTP 200
[Asserts]
jsonpath "$.status" == "WITHDRAWN"

# R4, R6: a REJECTED talk cannot be withdrawn, and stays REJECTED
POST {{base_url}}/talks/{{rejected}}/withdraw
{"speakerId": {{sparck_jones}}}
HTTP 409

GET {{base_url}}/talks/{{rejected}}
HTTP 200
[Asserts]
jsonpath "$.status" == "REJECTED"

# R4: nobody else can withdraw, also when the talk is REJECTED (403 before 409)
POST {{base_url}}/talks/{{rejected}}/withdraw
{"speakerId": {{perlman}}}
HTTP 403

# ================================================================ R7: a withdrawn talk does not count towards the limit

POST {{base_url}}/speakers
{"name": "Hedy Lamarr", "email": "hedy-{{newUuid}}@example.com"}
HTTP 201
[Captures]
lamarr: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Frequency hopping", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{lamarr}}}
HTTP 201

POST {{base_url}}/talks
{"title": "Player piano rolls", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{lamarr}}}
HTTP 201

POST {{base_url}}/talks
{"title": "Torpedo guidance", "format": "LIGHTNING_20", "level": "ADVANCED", "speakerId": {{lamarr}}}
HTTP 201
[Captures]
third_talk: jsonpath "$.id"

# R7: the third talk is SUBMITTED, a fourth is 409
POST {{base_url}}/talks
{"title": "One too many (submitted)", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{lamarr}}}
HTTP 409

# R7: the third talk is WAITLISTED, a fourth is 409
PUT {{base_url}}/talks/{{third_talk}}/status
{"status": "WAITLISTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "One too many (waitlisted)", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{lamarr}}}
HTTP 409

# R7: the third talk is ACCEPTED, a fourth is 409
PUT {{base_url}}/talks/{{third_talk}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{third_talk}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{third_talk}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "One too many (accepted)", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{lamarr}}}
HTTP 409

# R7: the third talk is WITHDRAWN, a fourth is 201
POST {{base_url}}/talks/{{third_talk}}/withdraw
{"speakerId": {{lamarr}}}
HTTP 200

POST {{base_url}}/talks
{"title": "Secret communication systems", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{lamarr}}}
HTTP 201
[Asserts]
jsonpath "$.status" == "SUBMITTED"

# R7: now 3 talks count again, a fifth is 409
POST {{base_url}}/talks
{"title": "One too many (again)", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{lamarr}}}
HTTP 409

POST {{base_url}}/speakers
{"name": "Evelyn Boyd Granville", "email": "evelyn-{{newUuid}}@example.com"}
HTTP 201
[Captures]
granville: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Orbit computations", "format": "PRESENTATION", "level": "ADVANCED", "speakerId": {{granville}}}
HTTP 201

POST {{base_url}}/talks
{"title": "Theory of computing", "format": "WORKSHOP", "level": "INTERMEDIATE", "speakerId": {{granville}}}
HTTP 201

POST {{base_url}}/talks
{"title": "Project Mercury", "format": "LIGHTNING_20", "level": "BEGINNER", "speakerId": {{granville}}}
HTTP 201
[Captures]
rejected_third: jsonpath "$.id"

# R7: the third talk is REJECTED, a fourth is 409
PUT {{base_url}}/talks/{{rejected_third}}/status
{"status": "REJECTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "One too many (rejected)", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{granville}}}
HTTP 409

# ================================================================ R8: ratings only for open talks

POST {{base_url}}/speakers
{"name": "Annie Easley", "email": "annie-{{newUuid}}@example.com"}
HTTP 201
[Captures]
easley: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Dorothy Vaughan", "email": "dorothy-{{newUuid}}@example.com"}
HTTP 201
[Captures]
vaughan: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Centaur rocket software", "format": "PRESENTATION", "level": "ADVANCED", "speakerId": {{easley}}}
HTTP 201
[Captures]
open_submitted: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Energy conversion systems", "format": "LIGHTNING_20", "level": "INTERMEDIATE", "speakerId": {{easley}}}
HTTP 201
[Captures]
open_waitlisted: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Battery technology", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{easley}}}
HTTP 201
[Captures]
closed_accepted: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "FORTRAN for computers", "format": "WORKSHOP", "level": "BEGINNER", "speakerId": {{vaughan}}}
HTTP 201
[Captures]
closed_rejected: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "West area computing", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{vaughan}}}
HTTP 201
[Captures]
closed_withdrawn: jsonpath "$.id"

PUT {{base_url}}/talks/{{open_submitted}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{open_submitted}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{open_waitlisted}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{open_waitlisted}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{closed_accepted}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{closed_accepted}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{closed_rejected}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{closed_rejected}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{closed_withdrawn}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{closed_withdrawn}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{open_waitlisted}}/status
{"status": "WAITLISTED"}
HTTP 200

PUT {{base_url}}/talks/{{closed_accepted}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{closed_rejected}}/status
{"status": "REJECTED"}
HTTP 200

POST {{base_url}}/talks/{{closed_withdrawn}}/withdraw
{"speakerId": {{vaughan}}}
HTTP 200

# R8: a SUBMITTED talk takes a third rating
PUT {{base_url}}/talks/{{open_submitted}}/ratings/3
{"score": 5}
HTTP 200

GET {{base_url}}/talks/{{open_submitted}}
HTTP 200
[Asserts]
jsonpath "$.ratingCount" == 3

# R8: a WAITLISTED talk takes a third rating
PUT {{base_url}}/talks/{{open_waitlisted}}/ratings/3
{"score": 5}
HTTP 200

GET {{base_url}}/talks/{{open_waitlisted}}
HTTP 200
[Asserts]
jsonpath "$.ratingCount" == 3

# R8: an ACCEPTED talk takes no new rating and no changed score
PUT {{base_url}}/talks/{{closed_accepted}}/ratings/3
{"score": 5}
HTTP 409

PUT {{base_url}}/talks/{{closed_accepted}}/ratings/1
{"score": 1}
HTTP 409

GET {{base_url}}/talks/{{closed_accepted}}
HTTP 200
[Asserts]
jsonpath "$.ratingCount" == 2
jsonpath "$.averageRating" == 4.0

# R8: a REJECTED talk takes no new rating and no changed score
PUT {{base_url}}/talks/{{closed_rejected}}/ratings/3
{"score": 5}
HTTP 409

PUT {{base_url}}/talks/{{closed_rejected}}/ratings/1
{"score": 1}
HTTP 409

GET {{base_url}}/talks/{{closed_rejected}}
HTTP 200
[Asserts]
jsonpath "$.ratingCount" == 2
jsonpath "$.averageRating" == 4.0

# R8: a WITHDRAWN talk takes no new rating and no changed score
PUT {{base_url}}/talks/{{closed_withdrawn}}/ratings/3
{"score": 5}
HTTP 409

PUT {{base_url}}/talks/{{closed_withdrawn}}/ratings/1
{"score": 1}
HTTP 409

GET {{base_url}}/talks/{{closed_withdrawn}}
HTTP 200
[Asserts]
jsonpath "$.ratingCount" == 2
jsonpath "$.averageRating" == 4.0

# ================================================================ R9: withdrawn talks in the lists

# Talks A, B and C are submitted in this order, with scores 5, 3 and 4 from different reviewers.
POST {{base_url}}/speakers
{"name": "Margaret Hamilton", "email": "margaret-{{newUuid}}@example.com"}
HTTP 201
[Captures]
hamilton: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "A: Software engineering", "format": "PRESENTATION", "level": "INTERMEDIATE", "speakerId": {{hamilton}}}
HTTP 201
[Captures]
talk_a: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "B: Priority displays", "format": "LIGHTNING_20", "level": "ADVANCED", "speakerId": {{hamilton}}}
HTTP 201
[Captures]
talk_b: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "C: Universal systems language", "format": "WORKSHOP", "level": "ADVANCED", "speakerId": {{hamilton}}}
HTTP 201
[Captures]
talk_c: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk_a}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk_b}}/ratings/2
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk_c}}/ratings/3
{"score": 4}
HTTP 200

# R9: no talk withdrawn, the lists are as before (oldest first; best average first)
GET {{base_url}}/talks
[Query]
speakerId: {{hamilton}}
HTTP 200
[Asserts]
jsonpath "$[*].id" count == 3
jsonpath "$[0].id" == {{talk_a}}
jsonpath "$[1].id" == {{talk_b}}
jsonpath "$[2].id" == {{talk_c}}

GET {{base_url}}/talks
[Query]
speakerId: {{hamilton}}
sort: rating
HTTP 200
[Asserts]
jsonpath "$[0].id" == {{talk_a}}
jsonpath "$[1].id" == {{talk_c}}
jsonpath "$[2].id" == {{talk_b}}

POST {{base_url}}/talks/{{talk_a}}/withdraw
{"speakerId": {{hamilton}}}
HTTP 200

# R9: A withdrawn, /talks shows B, C, A
GET {{base_url}}/talks
[Query]
speakerId: {{hamilton}}
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].title" == "B: Priority displays"
jsonpath "$[1].title" == "C: Universal systems language"
jsonpath "$[2].title" == "A: Software engineering"
jsonpath "$[2].status" == "WITHDRAWN"

# R9: A withdrawn, includeWithdrawn=true shows B, C, A
GET {{base_url}}/talks
[Query]
speakerId: {{hamilton}}
includeWithdrawn: true
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].id" == {{talk_b}}
jsonpath "$[1].id" == {{talk_c}}
jsonpath "$[2].id" == {{talk_a}}

# R9: A withdrawn, sort=rating shows C, B, A although A has the best average
GET {{base_url}}/talks
[Query]
speakerId: {{hamilton}}
sort: rating
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].id" == {{talk_c}}
jsonpath "$[1].id" == {{talk_b}}
jsonpath "$[2].id" == {{talk_a}}

# R9: A withdrawn, includeWithdrawn=false shows B, C
GET {{base_url}}/talks
[Query]
speakerId: {{hamilton}}
includeWithdrawn: false
HTTP 200
[Asserts]
jsonpath "$" count == 2
jsonpath "$[0].id" == {{talk_b}}
jsonpath "$[1].id" == {{talk_c}}

# R9: A withdrawn, sort=rating and includeWithdrawn=false shows C, B
GET {{base_url}}/talks
[Query]
speakerId: {{hamilton}}
sort: rating
includeWithdrawn: false
HTTP 200
[Asserts]
jsonpath "$" count == 2
jsonpath "$[0].id" == {{talk_c}}
jsonpath "$[1].id" == {{talk_b}}

# R9: includeWithdrawn other than true or false is 400
GET {{base_url}}/talks
[Query]
speakerId: {{hamilton}}
includeWithdrawn: yes
HTTP 400

# R9 (our decision): /speakers/{id}/talks does not change, A, B, C oldest first
GET {{base_url}}/speakers/{{hamilton}}/talks
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].id" == {{talk_a}}
jsonpath "$[1].id" == {{talk_b}}
jsonpath "$[2].id" == {{talk_c}}

POST {{base_url}}/talks/{{talk_b}}/withdraw
{"speakerId": {{hamilton}}}
HTTP 200

# R9: A and B withdrawn, sort=rating shows C, A, B
GET {{base_url}}/talks
[Query]
speakerId: {{hamilton}}
sort: rating
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].id" == {{talk_c}}
jsonpath "$[1].id" == {{talk_a}}
jsonpath "$[2].id" == {{talk_b}}

# R9 (our decision): A and B withdrawn, /talks shows C, A, B
GET {{base_url}}/talks
[Query]
speakerId: {{hamilton}}
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].id" == {{talk_c}}
jsonpath "$[1].id" == {{talk_a}}
jsonpath "$[2].id" == {{talk_b}}

# Talks A, B and C again: A scores 5, B has no rating, C scores 4.
POST {{base_url}}/speakers
{"name": "Katherine Johnson", "email": "katherine-{{newUuid}}@example.com"}
HTTP 201
[Captures]
johnson: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "A: Trajectory analysis", "format": "PRESENTATION", "level": "ADVANCED", "speakerId": {{johnson}}}
HTTP 201
[Captures]
unrated_a: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "B: Orbital equations", "format": "LIGHTNING_20", "level": "ADVANCED", "speakerId": {{johnson}}}
HTTP 201
[Captures]
unrated_b: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "C: Checking the computer", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{johnson}}}
HTTP 201
[Captures]
unrated_c: jsonpath "$.id"

PUT {{base_url}}/talks/{{unrated_a}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{unrated_c}}/ratings/3
{"score": 4}
HTTP 200

POST {{base_url}}/talks/{{unrated_a}}/withdraw
{"speakerId": {{johnson}}}
HTTP 200

# R9: A withdrawn and B unrated, sort=rating shows C, B, A (a withdrawn talk comes after an unrated one)
GET {{base_url}}/talks
[Query]
speakerId: {{johnson}}
sort: rating
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].id" == {{unrated_c}}
jsonpath "$[1].id" == {{unrated_b}}
jsonpath "$[1].ratingCount" == 0
jsonpath "$[2].id" == {{unrated_a}}

# R9: across all speakers, includeWithdrawn=false shows no WITHDRAWN talk
GET {{base_url}}/talks
[Query]
includeWithdrawn: false
HTTP 200
[Asserts]
jsonpath "$[*].status" not contains "WITHDRAWN"
jsonpath "$[*].id" not contains {{unrated_a}}
jsonpath "$[*].id" contains {{unrated_c}}

GET {{base_url}}/talks
[Query]
sort: rating
includeWithdrawn: false
HTTP 200
[Asserts]
jsonpath "$[*].status" not contains "WITHDRAWN"
