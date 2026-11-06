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
jsonpath "$[?(@.id == {{talk}})].coSpeakerIds[0]" == {{ada}}

GET {{base_url}}/talks?speakerId={{main}}
HTTP 200
[Asserts]
jsonpath "$[0].coSpeakerIds[1]" == {{bob}}

GET {{base_url}}/talks?sort=rating
HTTP 200
[Asserts]
jsonpath "$[?(@.id == {{talk}})].coSpeakerIds[1]" == {{bob}}

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
