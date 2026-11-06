# Our examples for Co-speakers (workflow/specs/co-speakers/spec.md). Drip features have no published examples.
# Run: examples/run.sh [base-url]

POST {{base_url}}/speakers
{"name": "Main", "email": "main-{{newUuid}}@example.com"}
HTTP 201
[Captures]
main: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Ada", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Bob", "email": "bob-{{newUuid}}@example.com"}
HTTP 201
[Captures]
bob: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Cy", "email": "cy-{{newUuid}}@example.com"}
HTTP 201
[Captures]
cy: jsonpath "$.id"

# R4: a new talk has no co-speakers
POST {{base_url}}/talks
{"title": "Pair presenting", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{main}}}
HTTP 201
[Captures]
talk: jsonpath "$.id"
[Asserts]
jsonpath "$.coSpeakerIds" count == 0

# R1: the first co-speaker
PUT {{base_url}}/talks/{{talk}}/co-speakers/{{ada}}
HTTP 200
[Asserts]
jsonpath "$.id" == {{talk}}
jsonpath "$.speakerId" == {{main}}
jsonpath "$.coSpeakerIds" count == 1
jsonpath "$.coSpeakerIds[0]" == {{ada}}

# R5: already a co-speaker changes nothing
PUT {{base_url}}/talks/{{talk}}/co-speakers/{{ada}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 1

# R1, R4: the second, in the order added
PUT {{base_url}}/talks/{{talk}}/co-speakers/{{bob}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 2
jsonpath "$.coSpeakerIds[0]" == {{ada}}
jsonpath "$.coSpeakerIds[1]" == {{bob}}

# R5: already one, at the limit
PUT {{base_url}}/talks/{{talk}}/co-speakers/{{bob}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 2

# R2: a third co-speaker
PUT {{base_url}}/talks/{{talk}}/co-speakers/{{cy}}
HTTP 409

# R3: the main speaker
PUT {{base_url}}/talks/{{talk}}/co-speakers/{{main}}
HTTP 409

# An unregistered speaker
PUT {{base_url}}/talks/{{talk}}/co-speakers/2147483647
HTTP 404

# A speaker id that is not a number
PUT {{base_url}}/talks/{{talk}}/co-speakers/abc
HTTP 400

# An unknown talk
PUT {{base_url}}/talks/2147483647/co-speakers/{{ada}}
HTTP 404

DELETE {{base_url}}/talks/2147483647/co-speakers/{{ada}}
HTTP 404

# A talk id that is not a number
PUT {{base_url}}/talks/abc/co-speakers/{{ada}}
HTTP 400

DELETE {{base_url}}/talks/abc/co-speakers/{{ada}}
HTTP 400

# R4: shown wherever a talk is shown
GET {{base_url}}/talks/{{talk}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds[0]" == {{ada}}
jsonpath "$.coSpeakerIds[1]" == {{bob}}

GET {{base_url}}/talks
HTTP 200
[Asserts]
jsonpath "$[?(@.id == {{talk}})].coSpeakerIds[*]" nth 0 == {{ada}}
jsonpath "$[?(@.id == {{talk}})].coSpeakerIds[*]" nth 1 == {{bob}}

GET {{base_url}}/talks?speakerId={{main}}
HTTP 200
[Asserts]
jsonpath "$[0].coSpeakerIds[1]" == {{bob}}

GET {{base_url}}/talks?sort=rating
HTTP 200
[Asserts]
jsonpath "$[?(@.id == {{talk}})].coSpeakerIds[*]" nth 0 == {{ada}}
jsonpath "$[?(@.id == {{talk}})].coSpeakerIds[*]" nth 1 == {{bob}}

GET {{base_url}}/talks?speakerId={{main}}&sort=rating
HTTP 200
[Asserts]
jsonpath "$[0].coSpeakerIds[0]" == {{ada}}

GET {{base_url}}/speakers/{{main}}/talks
HTTP 200
[Asserts]
jsonpath "$[0].coSpeakerIds[0]" == {{ada}}
jsonpath "$[0].coSpeakerIds[1]" == {{bob}}

# R6: a co-speaker's own talk list shows only their own talks
GET {{base_url}}/speakers/{{ada}}/talks
HTTP 200
[Asserts]
jsonpath "$" count == 0

GET {{base_url}}/talks?speakerId={{ada}}
HTTP 200
[Asserts]
jsonpath "$" count == 0

# R8: the main speaker can't be removed
DELETE {{base_url}}/talks/{{talk}}/co-speakers/{{main}}
HTTP 409

# R8: an unregistered speaker
DELETE {{base_url}}/talks/{{talk}}/co-speakers/2147483647
HTTP 404

# R8: a speaker id that is not a number
DELETE {{base_url}}/talks/{{talk}}/co-speakers/abc
HTTP 400

# R8: removing a registered speaker who is not a co-speaker changes nothing
DELETE {{base_url}}/talks/{{talk}}/co-speakers/{{cy}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 2

# R8: removing a co-speaker
DELETE {{base_url}}/talks/{{talk}}/co-speakers/{{ada}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 1
jsonpath "$.coSpeakerIds[0]" == {{bob}}

# R4: added again, goes last
PUT {{base_url}}/talks/{{talk}}/co-speakers/{{ada}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds[0]" == {{bob}}
jsonpath "$.coSpeakerIds[1]" == {{ada}}

# R7: a speaker with 3 talks of their own can still co-speak, and co-speaking doesn't count
POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada}}}
HTTP 201

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada}}}
HTTP 201

POST {{base_url}}/talks
{"title": "Ada three", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada}}}
HTTP 201

POST {{base_url}}/talks
{"title": "Second pair talk", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{main}}}
HTTP 201
[Captures]
second_talk: jsonpath "$.id"

PUT {{base_url}}/talks/{{second_talk}}/co-speakers/{{ada}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds[0]" == {{ada}}

# ================================================================ R4, R9: co-speakers and the talk's status
# Dee and Eve are main speakers. Their talks get each status through the acceptance endpoints.

POST {{base_url}}/speakers
{"name": "Dee", "email": "dee-{{newUuid}}@example.com"}
HTTP 201
[Captures]
dee: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Eve", "email": "eve-{{newUuid}}@example.com"}
HTTP 201
[Captures]
eve: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Still submitted", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{dee}}}
HTTP 201
[Captures]
submitted: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "On the waitlist", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{dee}}}
HTTP 201
[Captures]
waitlisted: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Accepted pair talk", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{dee}}}
HTTP 201
[Captures]
accepted: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Rejected pair talk", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{eve}}}
HTTP 201
[Captures]
rejected: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Withdrawn pair talk", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{eve}}}
HTTP 201
[Captures]
withdrawn: jsonpath "$.id"

PUT {{base_url}}/talks/{{waitlisted}}/co-speakers/{{ada}}
HTTP 200

PUT {{base_url}}/talks/{{rejected}}/co-speakers/{{ada}}
HTTP 200

PUT {{base_url}}/talks/{{withdrawn}}/co-speakers/{{ada}}
HTTP 200

# R4: the answer of PUT /talks/{id}/status shows the co-speakers, as the last field of the talk
PUT {{base_url}}/talks/{{waitlisted}}/status
{"status": "WAITLISTED"}
HTTP 200
[Asserts]
jsonpath "$.status" == "WAITLISTED"
jsonpath "$.coSpeakerIds" count == 1
jsonpath "$.coSpeakerIds[0]" == {{ada}}
body matches /"coSpeakerIds":\[\d+\]\}$/

PUT {{base_url}}/talks/{{rejected}}/status
{"status": "REJECTED"}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds[0]" == {{ada}}

# R4: the answer of POST /talks/{id}/withdraw shows the co-speakers, as the last field of the talk
POST {{base_url}}/talks/{{withdrawn}}/withdraw
{"speakerId": {{eve}}}
HTTP 200
[Asserts]
jsonpath "$.status" == "WITHDRAWN"
jsonpath "$.coSpeakerIds" count == 1
jsonpath "$.coSpeakerIds[0]" == {{ada}}
body matches /"coSpeakerIds":\[\d+\]\}$/

# R4: the list without withdrawn talks shows the co-speakers. Filtered on the main speaker, because
# an unfiltered list on the shared database is slow.
GET {{base_url}}/talks?speakerId={{dee}}&includeWithdrawn=false
HTTP 200
[Asserts]
jsonpath "$[?(@.id == {{waitlisted}})].coSpeakerIds[0]" == {{ada}}

GET {{base_url}}/talks?speakerId={{eve}}&includeWithdrawn=false
HTTP 200
[Asserts]
jsonpath "$" count == 1
jsonpath "$[0].id" == {{rejected}}
jsonpath "$[0].coSpeakerIds[0]" == {{ada}}

PUT {{base_url}}/talks/{{accepted}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{accepted}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{accepted}}/status
{"status": "ACCEPTED"}
HTTP 200

# R9: a WITHDRAWN talk gets no co-speaker, and keeps the ones it has
PUT {{base_url}}/talks/{{withdrawn}}/co-speakers/{{bob}}
HTTP 409

DELETE {{base_url}}/talks/{{withdrawn}}/co-speakers/{{ada}}
HTTP 409

GET {{base_url}}/talks/{{withdrawn}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 1
jsonpath "$.coSpeakerIds[0]" == {{ada}}

# R9: a REJECTED talk gets no co-speaker, and keeps the ones it has
PUT {{base_url}}/talks/{{rejected}}/co-speakers/{{bob}}
HTTP 409

DELETE {{base_url}}/talks/{{rejected}}/co-speakers/{{ada}}
HTTP 409

GET {{base_url}}/talks/{{rejected}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 1
jsonpath "$.coSpeakerIds[0]" == {{ada}}

# R9: a SUBMITTED talk can get and lose a co-speaker
PUT {{base_url}}/talks/{{submitted}}/co-speakers/{{bob}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds[0]" == {{bob}}

DELETE {{base_url}}/talks/{{submitted}}/co-speakers/{{bob}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 0

# R9: a WAITLISTED talk can get and lose a co-speaker
PUT {{base_url}}/talks/{{waitlisted}}/co-speakers/{{bob}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 2

DELETE {{base_url}}/talks/{{waitlisted}}/co-speakers/{{ada}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 1
jsonpath "$.coSpeakerIds[0]" == {{bob}}

# R9: an ACCEPTED talk can get and lose a co-speaker
PUT {{base_url}}/talks/{{accepted}}/co-speakers/{{bob}}
HTTP 200
[Asserts]
jsonpath "$.status" == "ACCEPTED"
jsonpath "$.coSpeakerIds[0]" == {{bob}}

DELETE {{base_url}}/talks/{{accepted}}/co-speakers/{{bob}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 0

# R12: without actingSpeakerId, anyone changes the co-speakers, as above. With it, only the main speaker.
PUT {{base_url}}/talks/{{second_talk}}/co-speakers/{{bob}}?actingSpeakerId={{main}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 2
jsonpath "$.coSpeakerIds[1]" == {{bob}}

# R12: a co-speaker, another speaker or an unregistered one can't, and nothing changes
PUT {{base_url}}/talks/{{second_talk}}/co-speakers/{{cy}}?actingSpeakerId={{ada}}
HTTP 403
[Asserts]
jsonpath "$.status" == 403
jsonpath "$.error" == "Forbidden"
jsonpath "$.message" == "only the main speaker can change the co-speakers"
jsonpath "$.path" == "/talks/{{second_talk}}/co-speakers/{{cy}}"

DELETE {{base_url}}/talks/{{second_talk}}/co-speakers/{{bob}}?actingSpeakerId={{cy}}
HTTP 403
[Asserts]
jsonpath "$.message" == "only the main speaker can change the co-speakers"

DELETE {{base_url}}/talks/{{second_talk}}/co-speakers/{{bob}}?actingSpeakerId=2147483647
HTTP 403

# R12: an acting id that is not a number
PUT {{base_url}}/talks/{{second_talk}}/co-speakers/{{cy}}?actingSpeakerId=abc
HTTP 400

DELETE {{base_url}}/talks/{{second_talk}}/co-speakers/{{bob}}?actingSpeakerId=abc
HTTP 400

# R12: an unknown talk is 404 before who acts is checked
PUT {{base_url}}/talks/2147483647/co-speakers/{{cy}}?actingSpeakerId={{cy}}
HTTP 404

GET {{base_url}}/talks/{{second_talk}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 2

# R12: the main speaker removes a co-speaker
DELETE {{base_url}}/talks/{{second_talk}}/co-speakers/{{bob}}?actingSpeakerId={{main}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 1
jsonpath "$.coSpeakerIds[0]" == {{ada}}
