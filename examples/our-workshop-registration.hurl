# Workshop registration with capacity: our black-box examples
# (workflow/specs/workshop-registration/spec.md).
# Run: examples/run.sh [base-url]
# Each group registers its own speakers and submits its own workshops, so the file runs against a
# shared database. A speaker has at most 3 talks. A workshop is ACCEPTED after ratings from two
# reviewers and the committee's decision (Acceptance R2, R3).
# Hurl sends the requests one at a time, so the rows "at the same time" of R5 and R6 are not here
# (WorkshopRegistrationTest has them).

# ================================================================ R1: a name and a real email

POST {{base_url}}/speakers
{"name": "Alan Turing", "email": "turing-{{newUuid}}@example.com"}
HTTP 201
[Captures]
turing: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Machines that think", "format": "WORKSHOP", "level": "INTERMEDIATE", "speakerId": {{turing}}}
HTTP 201
[Captures]
r1_workshop: jsonpath "$.id"

PUT {{base_url}}/talks/{{r1_workshop}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{r1_workshop}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{r1_workshop}}/status
{"status": "ACCEPTED"}
HTTP 200

# R1, R2, R3: a name and a real email for an accepted workshop, and the response is the registration
POST {{base_url}}/workshops/{{r1_workshop}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 201
[Captures]
r1_ada: jsonpath "$.id"
[Asserts]
jsonpath "$.id" isInteger
jsonpath "$.workshopId" == {{r1_workshop}}
jsonpath "$.name" == "Ada Lovelace"
jsonpath "$.email" == "ada@example.com"

# R1: the name is empty
POST {{base_url}}/workshops/{{r1_workshop}}/registrations
{"name": "", "email": "grace@example.com"}
HTTP 400

# R1: the name is only spaces
POST {{base_url}}/workshops/{{r1_workshop}}/registrations
{"name": "   ", "email": "grace@example.com"}
HTTP 400

# R1: the name is missing
POST {{base_url}}/workshops/{{r1_workshop}}/registrations
{"email": "grace@example.com"}
HTTP 400

# R1: the email is not a real address
POST {{base_url}}/workshops/{{r1_workshop}}/registrations
{"name": "Grace Hopper", "email": "not-an-email"}
HTTP 400

# R1: the email is empty
POST {{base_url}}/workshops/{{r1_workshop}}/registrations
{"name": "Grace Hopper", "email": ""}
HTTP 400

# R1: the email is only spaces
POST {{base_url}}/workshops/{{r1_workshop}}/registrations
{"name": "Grace Hopper", "email": "   "}
HTTP 400

# R1: the email is missing
POST {{base_url}}/workshops/{{r1_workshop}}/registrations
{"name": "Grace Hopper"}
HTTP 400

# Our decisions: malformed JSON and no body are 400
POST {{base_url}}/workshops/{{r1_workshop}}/registrations
Content-Type: application/json
```
{"name": "Grace Hopper", "email": "grace@example.com"
```
HTTP 400

POST {{base_url}}/workshops/{{r1_workshop}}/registrations
Content-Type: application/json
HTTP 400

# Our decisions: an ID in the path that is not a number is 400
POST {{base_url}}/workshops/abc/registrations
{"name": "Grace Hopper", "email": "grace@example.com"}
HTTP 400

GET {{base_url}}/workshops/abc/registrations
HTTP 400

# R1, R8: none of the bad requests saved a registration
GET {{base_url}}/workshops/{{r1_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 1
jsonpath "$[0].id" == {{r1_ada}}
jsonpath "$[0].workshopId" == {{r1_workshop}}
jsonpath "$[0].name" == "Ada Lovelace"
jsonpath "$[0].email" == "ada@example.com"

# ================================================================ R2: only a workshop

POST {{base_url}}/speakers
{"name": "Margaret Hamilton", "email": "hamilton-{{newUuid}}@example.com"}
HTTP 201
[Captures]
hamilton: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Priority displays", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{hamilton}}}
HTTP 201
[Captures]
lightning_10: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Software engineering, the name", "format": "LIGHTNING_20", "level": "BEGINNER", "speakerId": {{hamilton}}}
HTTP 201
[Captures]
lightning_20: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Apollo guidance", "format": "PRESENTATION", "level": "ADVANCED", "speakerId": {{hamilton}}}
HTTP 201
[Captures]
presentation: jsonpath "$.id"

PUT {{base_url}}/talks/{{lightning_10}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{lightning_10}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{lightning_10}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{lightning_20}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{lightning_20}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{lightning_20}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{presentation}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{presentation}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{presentation}}/status
{"status": "ACCEPTED"}
HTTP 200

# R2: an accepted LIGHTNING_10 is not a workshop
POST {{base_url}}/workshops/{{lightning_10}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 404

GET {{base_url}}/workshops/{{lightning_10}}/registrations
HTTP 404

# R2: an accepted LIGHTNING_20 is not a workshop
POST {{base_url}}/workshops/{{lightning_20}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 404

GET {{base_url}}/workshops/{{lightning_20}}/registrations
HTTP 404

# R2: an accepted PRESENTATION is not a workshop
POST {{base_url}}/workshops/{{presentation}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 404

GET {{base_url}}/workshops/{{presentation}}/registrations
HTTP 404

# R2: no talk with this ID
POST {{base_url}}/workshops/999999999/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 404

GET {{base_url}}/workshops/999999999/registrations
HTTP 404

# ================================================================ R3: only an accepted workshop

POST {{base_url}}/speakers
{"name": "Frances Allen", "email": "allen-{{newUuid}}@example.com"}
HTTP 201
[Captures]
allen: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Optimising compilers", "format": "WORKSHOP", "level": "ADVANCED", "speakerId": {{allen}}}
HTTP 201
[Captures]
submitted: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Program flow graphs", "format": "WORKSHOP", "level": "INTERMEDIATE", "speakerId": {{allen}}}
HTTP 201
[Captures]
waitlisted: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Parallel Fortran", "format": "WORKSHOP", "level": "BEGINNER", "speakerId": {{allen}}}
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
{"name": "Radia Perlman", "email": "perlman-{{newUuid}}@example.com"}
HTTP 201
[Captures]
perlman: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Spanning trees by hand", "format": "WORKSHOP", "level": "INTERMEDIATE", "speakerId": {{perlman}}}
HTTP 201
[Captures]
withdrawn: jsonpath "$.id"

POST {{base_url}}/talks/{{withdrawn}}/withdraw
{"speakerId": {{perlman}}}
HTTP 200

# R3: a SUBMITTED workshop takes no registration, and its list stays empty
POST {{base_url}}/workshops/{{submitted}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 409

GET {{base_url}}/workshops/{{submitted}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 0

# R3: a WAITLISTED workshop takes no registration, and its list stays empty
POST {{base_url}}/workshops/{{waitlisted}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 409

GET {{base_url}}/workshops/{{waitlisted}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 0

# R3: a REJECTED workshop takes no registration, and its list stays empty
POST {{base_url}}/workshops/{{rejected}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 409

GET {{base_url}}/workshops/{{rejected}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 0

# R3: a WITHDRAWN workshop takes no registration, and its list stays empty
POST {{base_url}}/workshops/{{withdrawn}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 409

GET {{base_url}}/workshops/{{withdrawn}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 0

# ================================================================ R4: a withdrawn workshop keeps its attendees

POST {{base_url}}/speakers
{"name": "Karen Sparck Jones", "email": "sparck-jones-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sparck_jones: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Inverse document frequency", "format": "WORKSHOP", "level": "INTERMEDIATE", "speakerId": {{sparck_jones}}}
HTTP 201
[Captures]
r4_workshop: jsonpath "$.id"

PUT {{base_url}}/talks/{{r4_workshop}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{r4_workshop}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{r4_workshop}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/workshops/{{r4_workshop}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 201

POST {{base_url}}/workshops/{{r4_workshop}}/registrations
{"name": "Grace Hopper", "email": "grace@example.com"}
HTTP 201

POST {{base_url}}/talks/{{r4_workshop}}/withdraw
{"speakerId": {{sparck_jones}}}
HTTP 200

# R4: the withdrawn workshop still lists Ada and Grace
GET {{base_url}}/workshops/{{r4_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 2
jsonpath "$[0].name" == "Ada Lovelace"
jsonpath "$[1].name" == "Grace Hopper"

# R3, R4: the withdrawn workshop takes no new registration
POST {{base_url}}/workshops/{{r4_workshop}}/registrations
{"name": "Barbara Liskov", "email": "barbara@example.com"}
HTTP 409

GET {{base_url}}/workshops/{{r4_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 2

# ================================================================ R5: 20 seats

POST {{base_url}}/speakers
{"name": "Hedy Lamarr", "email": "lamarr-{{newUuid}}@example.com"}
HTTP 201
[Captures]
lamarr: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Frequency hopping", "format": "WORKSHOP", "level": "BEGINNER", "speakerId": {{lamarr}}}
HTTP 201
[Captures]
r5_workshop: jsonpath "$.id"

PUT {{base_url}}/talks/{{r5_workshop}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{r5_workshop}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{r5_workshop}}/status
{"status": "ACCEPTED"}
HTTP 200

# R5: 19 attendees with different emails register
POST {{base_url}}/workshops/{{r5_workshop}}/registrations
[Options]
repeat: 19
{"name": "Attendee", "email": "attendee-{{newUuid}}@example.com"}
HTTP 201

GET {{base_url}}/workshops/{{r5_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 19

# R5: with 19 registered, the 20th attendee gets the last seat
POST {{base_url}}/workshops/{{r5_workshop}}/registrations
{"name": "Twentieth Attendee", "email": "twentieth@example.com"}
HTTP 201

# R5: with 20 registered, the 21st attendee gets no seat
POST {{base_url}}/workshops/{{r5_workshop}}/registrations
{"name": "Twenty-first Attendee", "email": "twenty-first@example.com"}
HTTP 409

GET {{base_url}}/workshops/{{r5_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 20
jsonpath "$[19].email" == "twentieth@example.com"
jsonpath "$[*].email" not contains "twenty-first@example.com"

# ================================================================ R6: one registration per email

POST {{base_url}}/speakers
{"name": "Sophie Wilson", "email": "wilson-{{newUuid}}@example.com"}
HTTP 201
[Captures]
wilson: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Designing an instruction set", "format": "WORKSHOP", "level": "ADVANCED", "speakerId": {{wilson}}}
HTTP 201
[Captures]
r6_workshop: jsonpath "$.id"

PUT {{base_url}}/talks/{{r6_workshop}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{r6_workshop}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{r6_workshop}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/workshops/{{r6_workshop}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 201

# R6: the same email again
POST {{base_url}}/workshops/{{r6_workshop}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 409

# R6: the same email in other letter case
POST {{base_url}}/workshops/{{r6_workshop}}/registrations
{"name": "Ada King", "email": "ADA@Example.com"}
HTTP 409

# R6: another email
POST {{base_url}}/workshops/{{r6_workshop}}/registrations
{"name": "Grace Hopper", "email": "grace@example.com"}
HTTP 201

GET {{base_url}}/workshops/{{r6_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 2
jsonpath "$[0].email" == "ada@example.com"
jsonpath "$[1].email" == "grace@example.com"

# ================================================================ R7: several workshops

POST {{base_url}}/speakers
{"name": "Joan Clarke", "email": "clarke-{{newUuid}}@example.com"}
HTTP 201
[Captures]
clarke: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Banburismus", "format": "WORKSHOP", "level": "ADVANCED", "speakerId": {{clarke}}}
HTTP 201
[Captures]
first_workshop: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Reading Enigma traffic", "format": "WORKSHOP", "level": "INTERMEDIATE", "speakerId": {{clarke}}}
HTTP 201
[Captures]
second_workshop: jsonpath "$.id"

PUT {{base_url}}/talks/{{first_workshop}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{first_workshop}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{first_workshop}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{second_workshop}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{second_workshop}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{second_workshop}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/workshops/{{first_workshop}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 201

# R7: Ada, registered for the first workshop, registers for the second
POST {{base_url}}/workshops/{{second_workshop}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 201
[Asserts]
jsonpath "$.workshopId" == {{second_workshop}}

GET {{base_url}}/workshops/{{first_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 1
jsonpath "$[0].workshopId" == {{first_workshop}}
jsonpath "$[0].email" == "ada@example.com"

GET {{base_url}}/workshops/{{second_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 1
jsonpath "$[0].workshopId" == {{second_workshop}}
jsonpath "$[0].email" == "ada@example.com"

# ================================================================ R8: the list in registration order

POST {{base_url}}/speakers
{"name": "Mary Jackson", "email": "jackson-{{newUuid}}@example.com"}
HTTP 201
[Captures]
jackson: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Wind tunnel data", "format": "WORKSHOP", "level": "BEGINNER", "speakerId": {{jackson}}}
HTTP 201
[Captures]
r8_workshop: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Supersonic flow", "format": "WORKSHOP", "level": "ADVANCED", "speakerId": {{jackson}}}
HTTP 201
[Captures]
empty_workshop: jsonpath "$.id"

PUT {{base_url}}/talks/{{r8_workshop}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{r8_workshop}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{r8_workshop}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{empty_workshop}}/ratings/1
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{empty_workshop}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{empty_workshop}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/workshops/{{r8_workshop}}/registrations
{"name": "Ada Lovelace", "email": "ADA@Example.com"}
HTTP 201
[Captures]
r8_ada: jsonpath "$.id"
[Asserts]
jsonpath "$.email" == "ADA@Example.com"

POST {{base_url}}/workshops/{{r8_workshop}}/registrations
{"name": "Grace Hopper", "email": "grace@example.com"}
HTTP 201
[Captures]
r8_grace: jsonpath "$.id"

POST {{base_url}}/workshops/{{r8_workshop}}/registrations
{"name": "Barbara Liskov", "email": "barbara@example.com"}
HTTP 201
[Captures]
r8_barbara: jsonpath "$.id"

# R8: Ada, Grace and Barbara, in the order they registered, and the email as typed
GET {{base_url}}/workshops/{{r8_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].id" == {{r8_ada}}
jsonpath "$[0].name" == "Ada Lovelace"
jsonpath "$[0].email" == "ADA@Example.com"
jsonpath "$[1].id" == {{r8_grace}}
jsonpath "$[1].name" == "Grace Hopper"
jsonpath "$[2].id" == {{r8_barbara}}
jsonpath "$[2].name" == "Barbara Liskov"

# R8: an accepted workshop with nobody registered lists nobody
GET {{base_url}}/workshops/{{empty_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 0

# ================================================================ R9: unregister frees the seat

POST {{base_url}}/speakers
{"name": "Katherine Johnson", "email": "johnson-{{newUuid}}@example.com"}
HTTP 201
[Captures]
johnson: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Orbital trajectories", "format": "WORKSHOP", "level": "ADVANCED", "speakerId": {{johnson}}}
HTTP 201
[Captures]
full_workshop: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Navigation by the stars", "format": "WORKSHOP", "level": "BEGINNER", "speakerId": {{johnson}}}
HTTP 201
[Captures]
twice_workshop: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Checking the computer", "format": "WORKSHOP", "level": "INTERMEDIATE", "speakerId": {{johnson}}}
HTTP 201
[Captures]
other_workshop: jsonpath "$.id"

PUT {{base_url}}/talks/{{full_workshop}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{full_workshop}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{full_workshop}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{twice_workshop}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{twice_workshop}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{twice_workshop}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{other_workshop}}/ratings/1
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{other_workshop}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{other_workshop}}/status
{"status": "ACCEPTED"}
HTTP 200

# R5, R9: Ada and 19 other attendees fill the workshop, and Barbara gets no seat
POST {{base_url}}/workshops/{{full_workshop}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 201
[Captures]
full_ada: jsonpath "$.id"

POST {{base_url}}/workshops/{{full_workshop}}/registrations
[Options]
repeat: 19
{"name": "Attendee", "email": "attendee-{{newUuid}}@example.com"}
HTTP 201

POST {{base_url}}/workshops/{{full_workshop}}/registrations
{"name": "Barbara Liskov", "email": "barbara@example.com"}
HTTP 409

# R9: Ada unregisters from the full workshop, with no body in the response
DELETE {{base_url}}/workshops/{{full_workshop}}/registrations/{{full_ada}}
HTTP 204
[Asserts]
bytes count == 0

# R9: the freed seat goes to Barbara at once
POST {{base_url}}/workshops/{{full_workshop}}/registrations
{"name": "Barbara Liskov", "email": "barbara@example.com"}
HTTP 201

# R5, R9: the workshop is full again
POST {{base_url}}/workshops/{{full_workshop}}/registrations
{"name": "Grace Hopper", "email": "grace@example.com"}
HTTP 409

GET {{base_url}}/workshops/{{full_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 20
jsonpath "$[*].email" not contains "ada@example.com"
jsonpath "$[*].email" contains "barbara@example.com"
jsonpath "$[*].email" not contains "grace@example.com"

POST {{base_url}}/workshops/{{twice_workshop}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 201
[Captures]
first_ada: jsonpath "$.id"

POST {{base_url}}/workshops/{{twice_workshop}}/registrations
{"name": "Grace Hopper", "email": "grace@example.com"}
HTTP 201

# R9: Ada unregisters, and the list no longer has her
DELETE {{base_url}}/workshops/{{twice_workshop}}/registrations/{{first_ada}}
HTTP 204

GET {{base_url}}/workshops/{{twice_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 1
jsonpath "$[0].name" == "Grace Hopper"

# R9: Ada unregisters a second time, and the second request finds nothing
DELETE {{base_url}}/workshops/{{twice_workshop}}/registrations/{{first_ada}}
HTTP 404

# R6, R9: Ada registers again after unregistering
POST {{base_url}}/workshops/{{twice_workshop}}/registrations
{"name": "Ada Lovelace", "email": "ADA@Example.com"}
HTTP 201
[Captures]
second_ada: jsonpath "$.id"

GET {{base_url}}/workshops/{{twice_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 2
jsonpath "$[0].name" == "Grace Hopper"
jsonpath "$[1].id" == {{second_ada}}
jsonpath "$[1].id" != {{first_ada}}
jsonpath "$[1].email" == "ADA@Example.com"

# R9: Ada's registration is for another workshop
DELETE {{base_url}}/workshops/{{other_workshop}}/registrations/{{second_ada}}
HTTP 404

# R9: no registration with this ID
DELETE {{base_url}}/workshops/{{other_workshop}}/registrations/999999999
HTTP 404

# R9: none of the refused requests removed Ada from her workshop
GET {{base_url}}/workshops/{{twice_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 2
jsonpath "$[1].id" == {{second_ada}}

# R2, R9: no talk with this ID
DELETE {{base_url}}/workshops/999999999/registrations/{{second_ada}}
HTTP 404

# Our decisions: an ID in the path that is not a number is 400
DELETE {{base_url}}/workshops/abc/registrations/{{second_ada}}
HTTP 400

DELETE {{base_url}}/workshops/{{twice_workshop}}/registrations/abc
HTTP 400

POST {{base_url}}/speakers
{"name": "Dorothy Vaughan", "email": "vaughan-{{newUuid}}@example.com"}
HTTP 201
[Captures]
vaughan: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "FORTRAN for computers", "format": "WORKSHOP", "level": "BEGINNER", "speakerId": {{vaughan}}}
HTTP 201
[Captures]
withdrawn_workshop: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "From human computers to IBM", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{vaughan}}}
HTTP 201
[Captures]
not_a_workshop: jsonpath "$.id"

# R2, R9: a talk that is not a workshop
DELETE {{base_url}}/workshops/{{not_a_workshop}}/registrations/{{second_ada}}
HTTP 404

PUT {{base_url}}/talks/{{withdrawn_workshop}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{withdrawn_workshop}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{withdrawn_workshop}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/workshops/{{withdrawn_workshop}}/registrations
{"name": "Ada Lovelace", "email": "ada@example.com"}
HTTP 201
[Captures]
withdrawn_ada: jsonpath "$.id"

POST {{base_url}}/workshops/{{withdrawn_workshop}}/registrations
{"name": "Grace Hopper", "email": "grace@example.com"}
HTTP 201

POST {{base_url}}/talks/{{withdrawn_workshop}}/withdraw
{"speakerId": {{vaughan}}}
HTTP 200

# R4, R9: Ada unregisters from the withdrawn workshop, and the list shows only Grace
DELETE {{base_url}}/workshops/{{withdrawn_workshop}}/registrations/{{withdrawn_ada}}
HTTP 204

GET {{base_url}}/workshops/{{withdrawn_workshop}}/registrations
HTTP 200
[Asserts]
jsonpath "$" count == 1
jsonpath "$[0].name" == "Grace Hopper"
