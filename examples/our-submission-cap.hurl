# Change request "submission cap": our black-box examples (workflow/specs/submission-cap-cr/spec.md).
# Run: examples/run.sh [base-url]
# Ada is a new speaker in each group, registered with a unique email, so the file runs against a shared
# database. "Ada's talks" are the talks in the running (SUBMITTED, WAITLISTED or ACCEPTED) where Ada is
# the main speaker or a co-speaker. Zed owns the talks that Ada co-speaks on.
# Hurl sends one request at a time, so R6 (at the same time) is not here, and R7 (over the limit from
# before the change) cannot be set up through the API any more.

# ================================================================ R1, R2, R4: submitting a talk

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada1}}}
HTTP 201
[Captures]
a1_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada1}}}
HTTP 201
[Captures]
a1_2: jsonpath "$.id"

# R4: Ada has 2 talks of her own, so a third is 201
POST {{base_url}}/talks
{"title": "Ada three", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada1}}}
HTTP 201
[Captures]
a1_3: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada2: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Zed", "email": "zed-{{newUuid}}@example.com"}
HTTP 201
[Captures]
zed2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada2}}}
HTTP 201
[Captures]
a2_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada2}}}
HTTP 201
[Captures]
a2_2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Zed with Ada", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed2}}}
HTTP 201
[Captures]
z2: jsonpath "$.id"

PUT {{base_url}}/talks/{{z2}}/co-speakers/{{ada2}}
HTTP 200

# R1, R4: Ada has 2 talks of her own and co-speaks on 1, so a new talk is 409
POST {{base_url}}/talks
{"title": "Ada three", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada2}}}
HTTP 409

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada3: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Zed", "email": "zed-{{newUuid}}@example.com"}
HTTP 201
[Captures]
zed3: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada3}}}
HTTP 201
[Captures]
a3_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Zed one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed3}}}
HTTP 201
[Captures]
z3_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Zed two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed3}}}
HTTP 201
[Captures]
z3_2: jsonpath "$.id"

PUT {{base_url}}/talks/{{z3_1}}/co-speakers/{{ada3}}
HTTP 200

PUT {{base_url}}/talks/{{z3_2}}/co-speakers/{{ada3}}
HTTP 200

# R1, R4: Ada has 1 talk of her own and co-speaks on 2, so a new talk is 409
POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada3}}}
HTTP 409

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada4: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada4}}}
HTTP 201
[Captures]
a4_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada4}}}
HTTP 201
[Captures]
a4_2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada three", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada4}}}
HTTP 201
[Captures]
a4_3: jsonpath "$.id"

PUT {{base_url}}/talks/{{a4_3}}/status
{"status": "REJECTED"}
HTTP 200

# R2, R4: one of her 3 talks is REJECTED and does not count, so a new talk is 201
POST {{base_url}}/talks
{"title": "Ada four", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada4}}}
HTTP 201
[Captures]
a4_4: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada5: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada5}}}
HTTP 201
[Captures]
a5_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada5}}}
HTTP 201
[Captures]
a5_2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada three", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada5}}}
HTTP 201
[Captures]
a5_3: jsonpath "$.id"

POST {{base_url}}/talks/{{a5_3}}/withdraw
{"speakerId": {{ada5}}}
HTTP 200

# R2, R4: one of her 3 talks is WITHDRAWN and does not count, so a new talk is 201
POST {{base_url}}/talks
{"title": "Ada four", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada5}}}
HTTP 201
[Captures]
a5_4: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada60: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Zed", "email": "zed-{{newUuid}}@example.com"}
HTTP 201
[Captures]
zed60: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada60}}}
HTTP 201
[Captures]
ada60_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada60}}}
HTTP 201
[Captures]
ada60_2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Zed with Ada", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed60}}}
HTTP 201
[Captures]
z60: jsonpath "$.id"

PUT {{base_url}}/talks/{{z60}}/co-speakers/{{ada60}}
HTTP 200

PUT {{base_url}}/talks/{{z60}}/status
{"status": "REJECTED"}
HTTP 200

# R2, R4: Ada co-speaks on a REJECTED talk, which does not count, so a third talk of her own is 201
POST {{base_url}}/talks
{"title": "Ada three", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada60}}}
HTTP 201
[Captures]
ada60_3: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada61: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Zed", "email": "zed-{{newUuid}}@example.com"}
HTTP 201
[Captures]
zed61: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada61}}}
HTTP 201
[Captures]
ada61_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada61}}}
HTTP 201
[Captures]
ada61_2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Zed with Ada", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed61}}}
HTTP 201
[Captures]
z61: jsonpath "$.id"

PUT {{base_url}}/talks/{{z61}}/co-speakers/{{ada61}}
HTTP 200

POST {{base_url}}/talks/{{z61}}/withdraw
{"speakerId": {{zed61}}}
HTTP 200

# R2, R4: Ada co-speaks on a WITHDRAWN talk, which does not count, so a third talk of her own is 201
POST {{base_url}}/talks
{"title": "Ada three", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada61}}}
HTTP 201
[Captures]
ada61_3: jsonpath "$.id"

# ================================================================ R1, R5: adding Ada as co-speaker

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada7: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Zed", "email": "zed-{{newUuid}}@example.com"}
HTTP 201
[Captures]
zed7: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada7}}}
HTTP 201
[Captures]
a7_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada7}}}
HTTP 201
[Captures]
a7_2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Still submitted", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed7}}}
HTTP 201
[Captures]
z7_sub: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "On the waitlist", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed7}}}
HTTP 201
[Captures]
z7_wait: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Accepted", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed7}}}
HTTP 201
[Captures]
z7_acc: jsonpath "$.id"

PUT {{base_url}}/talks/{{z7_wait}}/status
{"status": "WAITLISTED"}
HTTP 200

PUT {{base_url}}/talks/{{z7_acc}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{z7_acc}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{z7_acc}}/status
{"status": "ACCEPTED"}
HTTP 200

# R5: Ada has 2 talks of her own, so she can co-speak on a SUBMITTED talk
PUT {{base_url}}/talks/{{z7_sub}}/co-speakers/{{ada7}}
HTTP 200

DELETE {{base_url}}/talks/{{z7_sub}}/co-speakers/{{ada7}}
HTTP 200

# R5: Ada has 2 talks of her own, so she can co-speak on a WAITLISTED talk
PUT {{base_url}}/talks/{{z7_wait}}/co-speakers/{{ada7}}
HTTP 200

DELETE {{base_url}}/talks/{{z7_wait}}/co-speakers/{{ada7}}
HTTP 200

# R5: Ada has 2 talks of her own, so she can co-speak on a ACCEPTED talk
PUT {{base_url}}/talks/{{z7_acc}}/co-speakers/{{ada7}}
HTTP 200

DELETE {{base_url}}/talks/{{z7_acc}}/co-speakers/{{ada7}}
HTTP 200

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada8: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada8}}}
HTTP 201
[Captures]
a8_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada8}}}
HTTP 201
[Captures]
a8_2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada three", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada8}}}
HTTP 201
[Captures]
a8_3: jsonpath "$.id"

# R1, R5: Ada has 3 talks of her own, so she is not added to a SUBMITTED talk
PUT {{base_url}}/talks/{{z7_sub}}/co-speakers/{{ada8}}
HTTP 409

# R1, R5: Ada has 3 talks of her own, so she is not added to a WAITLISTED talk
PUT {{base_url}}/talks/{{z7_wait}}/co-speakers/{{ada8}}
HTTP 409

# R1, R5: Ada has 3 talks of her own, so she is not added to a ACCEPTED talk
PUT {{base_url}}/talks/{{z7_acc}}/co-speakers/{{ada8}}
HTTP 409

# R5: none of the refused additions changed the talks
GET {{base_url}}/talks/{{z7_sub}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 0

GET {{base_url}}/talks/{{z7_wait}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 0

GET {{base_url}}/talks/{{z7_acc}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 0

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada9: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Zed", "email": "zed-{{newUuid}}@example.com"}
HTTP 201
[Captures]
zed9: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada9}}}
HTTP 201
[Captures]
a9_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada9}}}
HTTP 201
[Captures]
a9_2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Zed one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed9}}}
HTTP 201
[Captures]
z9_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Zed two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed9}}}
HTTP 201
[Captures]
z9_2: jsonpath "$.id"

PUT {{base_url}}/talks/{{z9_1}}/co-speakers/{{ada9}}
HTTP 200

# R1, R5: Ada has 2 talks of her own and co-speaks on 1, so she is not added to another
PUT {{base_url}}/talks/{{z9_2}}/co-speakers/{{ada9}}
HTTP 409

# Our decisions: Ada at the limit is added again to a talk she co-speaks on: 200, nothing changes (Co-speakers R5)
PUT {{base_url}}/talks/{{z9_1}}/co-speakers/{{ada9}}
HTTP 200

GET {{base_url}}/talks/{{z9_1}}
HTTP 200
[Asserts]
jsonpath "$.coSpeakerIds" count == 1
jsonpath "$.coSpeakerIds[0]" == {{ada9}}

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada10: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Zed", "email": "zed-{{newUuid}}@example.com"}
HTTP 201
[Captures]
zed10: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada10}}}
HTTP 201
[Captures]
a10_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada10}}}
HTTP 201
[Captures]
a10_2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada three", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada10}}}
HTTP 201
[Captures]
a10_3: jsonpath "$.id"

PUT {{base_url}}/talks/{{a10_3}}/status
{"status": "REJECTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Zed one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed10}}}
HTTP 201
[Captures]
z10: jsonpath "$.id"

# R2, R5: one of her 3 talks is REJECTED, so she can co-speak on a SUBMITTED talk
PUT {{base_url}}/talks/{{z10}}/co-speakers/{{ada10}}
HTTP 200

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada11: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Zed", "email": "zed-{{newUuid}}@example.com"}
HTTP 201
[Captures]
zed11: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada11}}}
HTTP 201
[Captures]
a11_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada11}}}
HTTP 201
[Captures]
a11_2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada three", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada11}}}
HTTP 201
[Captures]
a11_3: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Zed rejected", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed11}}}
HTTP 201
[Captures]
z11_rej: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Zed withdrawn", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed11}}}
HTTP 201
[Captures]
z11_wd: jsonpath "$.id"

PUT {{base_url}}/talks/{{z11_rej}}/status
{"status": "REJECTED"}
HTTP 200

POST {{base_url}}/talks/{{z11_wd}}/withdraw
{"speakerId": {{zed11}}}
HTTP 200

# Co-speakers R9 (the product owner's answer to open question 1): a REJECTED talk gets no co-speaker
PUT {{base_url}}/talks/{{z11_rej}}/co-speakers/{{ada11}}
HTTP 409

# Co-speakers R9: a WITHDRAWN talk gets no co-speaker
PUT {{base_url}}/talks/{{z11_wd}}/co-speakers/{{ada11}}
HTTP 409

# ================================================================ R3: a freed place can be used at once

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada12: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Zed", "email": "zed-{{newUuid}}@example.com"}
HTTP 201
[Captures]
zed12: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada12}}}
HTTP 201
[Captures]
a12_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada12}}}
HTTP 201
[Captures]
a12_2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Zed with Ada", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed12}}}
HTTP 201
[Captures]
z12: jsonpath "$.id"

PUT {{base_url}}/talks/{{z12}}/co-speakers/{{ada12}}
HTTP 200

# R4: Ada is at the limit
POST {{base_url}}/talks
{"title": "Ada three", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada12}}}
HTTP 409

# R3: Ada is taken off the talk as co-speaker, which frees her place
DELETE {{base_url}}/talks/{{z12}}/co-speakers/{{ada12}}
HTTP 200

# R3: the freed place is used at once
POST {{base_url}}/talks
{"title": "Ada three", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada12}}}
HTTP 201
[Captures]
a12_3: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada13: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Zed", "email": "zed-{{newUuid}}@example.com"}
HTTP 201
[Captures]
zed13: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada13}}}
HTTP 201
[Captures]
a13_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada two", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada13}}}
HTTP 201
[Captures]
a13_2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada three", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada13}}}
HTTP 201
[Captures]
a13_3: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Zed one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed13}}}
HTTP 201
[Captures]
z13: jsonpath "$.id"

# R5: Ada is at the limit
PUT {{base_url}}/talks/{{z13}}/co-speakers/{{ada13}}
HTTP 409

# R3: Ada withdraws one of her talks, which frees her place
POST {{base_url}}/talks/{{a13_3}}/withdraw
{"speakerId": {{ada13}}}
HTTP 200

# R3: the freed place is used at once
PUT {{base_url}}/talks/{{z13}}/co-speakers/{{ada13}}
HTTP 200

# ================================================================ R8: a speaker's talk list shows only the talks where they are the main speaker

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
ada14: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Zed", "email": "zed-{{newUuid}}@example.com"}
HTTP 201
[Captures]
zed14: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Ada one", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{ada14}}}
HTTP 201
[Captures]
a14_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Zed with Ada", "format": "LIGHTNING_10", "level": "BEGINNER", "speakerId": {{zed14}}}
HTTP 201
[Captures]
z14: jsonpath "$.id"

PUT {{base_url}}/talks/{{z14}}/co-speakers/{{ada14}}
HTTP 200

# R8: Ada co-speaks on Zed's talk, and her own list has only her own talk
GET {{base_url}}/speakers/{{ada14}}/talks
HTTP 200
[Asserts]
jsonpath "$" count == 1
jsonpath "$[0].id" == {{a14_1}}

GET {{base_url}}/talks?speakerId={{ada14}}
HTTP 200
[Asserts]
jsonpath "$" count == 1
jsonpath "$[0].id" == {{a14_1}}
