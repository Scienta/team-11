# Programme export: our black-box examples (workflow/specs/programme-export/spec.md).
# Changes the shared programme: run only against a local build, never against the team URL.
# Run: hurl --test --variable base_url=http://localhost:8080 examples/local/our-programme-export.hurl
# examples/run.sh does not run it, because run.sh takes only the files directly in examples/.
# The export shows the whole programme, so each group first empties all 15 places, and takes its
# talks out again at the end. Each group registers its own speakers. A talk is ACCEPTED after
# ratings from two reviewers and the committee's decision (Acceptance R2, R3).

# ================================================================ R1, R7: one row per talk on the programme, in both views

POST {{base_url}}/speakers
{"name": "Grace Hopper", "email": "hopper-{{newUuid}}@example.com"}
HTTP 201
[Captures]
g1: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "lovelace-{{newUuid}}@example.com"}
HTTP 201
[Captures]
a1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Accepted, not on the programme", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{a1}}}
HTTP 201
[Captures]
only_accepted: jsonpath "$.id"

PUT {{base_url}}/talks/{{only_accepted}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{only_accepted}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{only_accepted}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Still submitted", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{a1}}}
HTTP 201
[Captures]
only_submitted: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Taken out again", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{a1}}}
HTTP 201
[Captures]
taken_out: jsonpath "$.id"

PUT {{base_url}}/talks/{{taken_out}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{taken_out}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{taken_out}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Legacy is a feeling", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{g1}}}
HTTP 201
[Captures]
r1_a: jsonpath "$.id"

PUT {{base_url}}/talks/{{r1_a}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{r1_a}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{r1_a}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Compilers for business", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{g1}}}
HTTP 201
[Captures]
r1_b: jsonpath "$.id"

PUT {{base_url}}/talks/{{r1_b}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{r1_b}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{r1_b}}/status
{"status": "ACCEPTED"}
HTTP 200

# Setup: empty the whole programme (204 if a talk was there, 404 if not)
DELETE {{base_url}}/programme/R1/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S3
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S3
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

DELETE {{base_url}}/programme/R2/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

# R7: an empty programme is an empty JSON list
GET {{base_url}}/programme
HTTP 200
[Asserts]
header "Content-Type" == "application/json;charset=UTF-8"
jsonpath "$" count == 0

# R5, R7: an empty programme is a CSV with only the header line
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\n"

PUT {{base_url}}/programme/R1/S2
{"talkId": {{taken_out}}}
HTTP 200

DELETE {{base_url}}/programme/R1/S2
HTTP 204

# R1, R7: accepted, submitted and taken-out talks are not on the programme
GET {{base_url}}/programme
HTTP 200
[Asserts]
jsonpath "$" count == 0

# R1, R7: the CSV has only the header line
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\n"

PUT {{base_url}}/programme/R1/S1
{"talkId": {{r1_a}}}
HTTP 200

# R1, R3: one row, exactly the eight fields of a ProgrammeRow, in order (Our decisions)
GET {{base_url}}/programme
HTTP 200
[Asserts]
body matches "^\\[\\{\"slot\":\"S1\",\"start\":\"09:00\",\"end\":\"10:00\",\"room\":\"R1\",\"talkId\":{{r1_a}},\"title\":\"Legacy is a feeling\",\"format\":\"PRESENTATION\",\"speakers\":\\[\"Grace Hopper\"\\]\\}\\]$"

# R1, R3: one line after the header
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{r1_a}},Legacy is a feeling,PRESENTATION,Grace Hopper\n"

PUT {{base_url}}/programme/R2/S3
{"talkId": {{r1_b}}}
HTTP 200

# R1: two rows, A then B
GET {{base_url}}/programme
HTTP 200
[Asserts]
header "Content-Type" == "application/json;charset=UTF-8"
jsonpath "$" count == 2
jsonpath "$[0].slot" == "S1"
jsonpath "$[0].start" == "09:00"
jsonpath "$[0].end" == "10:00"
jsonpath "$[0].room" == "R1"
jsonpath "$[0].talkId" == {{r1_a}}
jsonpath "$[0].title" == "Legacy is a feeling"
jsonpath "$[0].format" == "PRESENTATION"
jsonpath "$[0].speakers" count == 1
jsonpath "$[0].speakers[0]" == "Grace Hopper"
jsonpath "$[1].slot" == "S3"
jsonpath "$[1].start" == "11:30"
jsonpath "$[1].end" == "12:30"
jsonpath "$[1].room" == "R2"
jsonpath "$[1].talkId" == {{r1_b}}
jsonpath "$[1].title" == "Compilers for business"
jsonpath "$[1].format" == "PRESENTATION"
jsonpath "$[1].speakers" count == 1
jsonpath "$[1].speakers[0]" == "Grace Hopper"

# R1: two lines in the same order
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{r1_a}},Legacy is a feeling,PRESENTATION,Grace Hopper\nS3,11:30,12:30,R2,{{r1_b}},Compilers for business,PRESENTATION,Grace Hopper\n"

DELETE {{base_url}}/programme/R1/S1
HTTP 204

DELETE {{base_url}}/programme/R2/S3
HTTP 204

# ================================================================ R2: the order of a day

POST {{base_url}}/speakers
{"name": "Grace Hopper", "email": "hopper-{{newUuid}}@example.com"}
HTTP 201
[Captures]
o1: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "lovelace-{{newUuid}}@example.com"}
HTTP 201
[Captures]
o2: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Barbara Liskov", "email": "liskov-{{newUuid}}@example.com"}
HTTP 201
[Captures]
o3: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "First in R3", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{o1}}}
HTTP 201
[Captures]
t1: jsonpath "$.id"

PUT {{base_url}}/talks/{{t1}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{t1}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{t1}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Second slot", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{o2}}}
HTTP 201
[Captures]
t2: jsonpath "$.id"

PUT {{base_url}}/talks/{{t2}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{t2}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{t2}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "First in R1", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{o3}}}
HTTP 201
[Captures]
t3: jsonpath "$.id"

PUT {{base_url}}/talks/{{t3}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{t3}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{t3}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "First in R2", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{o2}}}
HTTP 201
[Captures]
t4: jsonpath "$.id"

PUT {{base_url}}/talks/{{t4}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{t4}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{t4}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Last of the day", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{o1}}}
HTTP 201
[Captures]
t5: jsonpath "$.id"

PUT {{base_url}}/talks/{{t5}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{t5}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{t5}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Long one in R3", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{o2}}}
HTTP 201
[Captures]
t6: jsonpath "$.id"

PUT {{base_url}}/talks/{{t6}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{t6}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{t6}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Long one in R2", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{o3}}}
HTTP 201
[Captures]
t7: jsonpath "$.id"

PUT {{base_url}}/talks/{{t7}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{t7}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{t7}}/status
{"status": "ACCEPTED"}
HTTP 200

# Setup: empty the whole programme (204 if a talk was there, 404 if not)
DELETE {{base_url}}/programme/R1/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S3
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S3
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

DELETE {{base_url}}/programme/R2/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

PUT {{base_url}}/programme/R3/S1
{"talkId": {{t1}}}
HTTP 200

PUT {{base_url}}/programme/R1/S2
{"talkId": {{t2}}}
HTTP 200

PUT {{base_url}}/programme/R1/S1
{"talkId": {{t3}}}
HTTP 200

PUT {{base_url}}/programme/R2/S1
{"talkId": {{t4}}}
HTTP 200

# R2: put in R3 S1, R1 S2, R1 S1, R2 S1; shown as R1 S1, R2 S1, R3 S1, R1 S2
GET {{base_url}}/programme
HTTP 200
[Asserts]
header "Content-Type" == "application/json;charset=UTF-8"
jsonpath "$" count == 4
jsonpath "$[0].slot" == "S1"
jsonpath "$[0].start" == "09:00"
jsonpath "$[0].end" == "10:00"
jsonpath "$[0].room" == "R1"
jsonpath "$[0].talkId" == {{t3}}
jsonpath "$[0].title" == "First in R1"
jsonpath "$[0].format" == "PRESENTATION"
jsonpath "$[0].speakers" count == 1
jsonpath "$[0].speakers[0]" == "Barbara Liskov"
jsonpath "$[1].slot" == "S1"
jsonpath "$[1].start" == "09:00"
jsonpath "$[1].end" == "10:00"
jsonpath "$[1].room" == "R2"
jsonpath "$[1].talkId" == {{t4}}
jsonpath "$[1].title" == "First in R2"
jsonpath "$[1].format" == "PRESENTATION"
jsonpath "$[1].speakers" count == 1
jsonpath "$[1].speakers[0]" == "Ada Lovelace"
jsonpath "$[2].slot" == "S1"
jsonpath "$[2].start" == "09:00"
jsonpath "$[2].end" == "10:00"
jsonpath "$[2].room" == "R3"
jsonpath "$[2].talkId" == {{t1}}
jsonpath "$[2].title" == "First in R3"
jsonpath "$[2].format" == "PRESENTATION"
jsonpath "$[2].speakers" count == 1
jsonpath "$[2].speakers[0]" == "Grace Hopper"
jsonpath "$[3].slot" == "S2"
jsonpath "$[3].start" == "10:15"
jsonpath "$[3].end" == "11:15"
jsonpath "$[3].room" == "R1"
jsonpath "$[3].talkId" == {{t2}}
jsonpath "$[3].title" == "Second slot"
jsonpath "$[3].format" == "PRESENTATION"
jsonpath "$[3].speakers" count == 1
jsonpath "$[3].speakers[0]" == "Ada Lovelace"

# R1, R2: the CSV in the same order
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{t3}},First in R1,PRESENTATION,Barbara Liskov\nS1,09:00,10:00,R2,{{t4}},First in R2,PRESENTATION,Ada Lovelace\nS1,09:00,10:00,R3,{{t1}},First in R3,PRESENTATION,Grace Hopper\nS2,10:15,11:15,R1,{{t2}},Second slot,PRESENTATION,Ada Lovelace\n"

DELETE {{base_url}}/programme/R3/S1
HTTP 204

DELETE {{base_url}}/programme/R1/S2
HTTP 204

DELETE {{base_url}}/programme/R1/S1
HTTP 204

DELETE {{base_url}}/programme/R2/S1
HTTP 204

PUT {{base_url}}/programme/R1/S5
{"talkId": {{t5}}}
HTTP 200

PUT {{base_url}}/programme/R3/S4
{"talkId": {{t6}}}
HTTP 200

PUT {{base_url}}/programme/R2/S4
{"talkId": {{t7}}}
HTTP 200

# R2: put in R1 S5, R3 S4, R2 S4; shown as R2 S4, R3 S4, R1 S5
GET {{base_url}}/programme
HTTP 200
[Asserts]
header "Content-Type" == "application/json;charset=UTF-8"
jsonpath "$" count == 3
jsonpath "$[0].slot" == "S4"
jsonpath "$[0].start" == "13:30"
jsonpath "$[0].end" == "15:30"
jsonpath "$[0].room" == "R2"
jsonpath "$[0].talkId" == {{t7}}
jsonpath "$[0].title" == "Long one in R2"
jsonpath "$[0].format" == "PRESENTATION"
jsonpath "$[0].speakers" count == 1
jsonpath "$[0].speakers[0]" == "Barbara Liskov"
jsonpath "$[1].slot" == "S4"
jsonpath "$[1].start" == "13:30"
jsonpath "$[1].end" == "15:30"
jsonpath "$[1].room" == "R3"
jsonpath "$[1].talkId" == {{t6}}
jsonpath "$[1].title" == "Long one in R3"
jsonpath "$[1].format" == "PRESENTATION"
jsonpath "$[1].speakers" count == 1
jsonpath "$[1].speakers[0]" == "Ada Lovelace"
jsonpath "$[2].slot" == "S5"
jsonpath "$[2].start" == "15:45"
jsonpath "$[2].end" == "16:45"
jsonpath "$[2].room" == "R1"
jsonpath "$[2].talkId" == {{t5}}
jsonpath "$[2].title" == "Last of the day"
jsonpath "$[2].format" == "PRESENTATION"
jsonpath "$[2].speakers" count == 1
jsonpath "$[2].speakers[0]" == "Grace Hopper"

# R1, R2: the CSV in the same order
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS4,13:30,15:30,R2,{{t7}},Long one in R2,PRESENTATION,Barbara Liskov\nS4,13:30,15:30,R3,{{t6}},Long one in R3,PRESENTATION,Ada Lovelace\nS5,15:45,16:45,R1,{{t5}},Last of the day,PRESENTATION,Grace Hopper\n"

DELETE {{base_url}}/programme/R1/S5
HTTP 204

DELETE {{base_url}}/programme/R3/S4
HTTP 204

DELETE {{base_url}}/programme/R2/S4
HTTP 204

# ================================================================ R3: what a row shows, as the talk is now

POST {{base_url}}/speakers
{"name": "Grace Hopper", "email": "hopper-{{newUuid}}@example.com"}
HTTP 201
[Captures]
g3: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Grace Hopper", "email": "hopper-{{newUuid}}@example.com"}
HTTP 201
[Captures]
g3b: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "lovelace-{{newUuid}}@example.com"}
HTTP 201
[Captures]
a3: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Hands on", "format": "WORKSHOP", "level": "BEGINNER", "speakerId": {{g3}}}
HTTP 201
[Captures]
hands_on: jsonpath "$.id"

PUT {{base_url}}/talks/{{hands_on}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{hands_on}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{hands_on}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Legacy is a feeling", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{g3}}}
HTTP 201
[Captures]
to_withdraw: jsonpath "$.id"

PUT {{base_url}}/talks/{{to_withdraw}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{to_withdraw}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{to_withdraw}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Legacy is a feeling", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{g3b}}}
HTTP 201
[Captures]
co_added: jsonpath "$.id"

PUT {{base_url}}/talks/{{co_added}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{co_added}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{co_added}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Legacy is a feeling", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{g3b}}}
HTTP 201
[Captures]
co_removed: jsonpath "$.id"

PUT {{base_url}}/talks/{{co_removed}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{co_removed}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{co_removed}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{co_removed}}/co-speakers/{{a3}}
HTTP 200

# Setup: empty the whole programme (204 if a talk was there, 404 if not)
DELETE {{base_url}}/programme/R1/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S3
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S3
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

DELETE {{base_url}}/programme/R2/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

PUT {{base_url}}/programme/R2/S4
{"talkId": {{hands_on}}}
HTTP 200

PUT {{base_url}}/programme/R3/S5
{"talkId": {{to_withdraw}}}
HTTP 200

PUT {{base_url}}/programme/R1/S2
{"talkId": {{co_added}}}
HTTP 200

PUT {{base_url}}/programme/R1/S3
{"talkId": {{co_removed}}}
HTTP 200

POST {{base_url}}/talks/{{to_withdraw}}/withdraw
{"speakerId": {{g3}}}
HTTP 200

PUT {{base_url}}/talks/{{co_added}}/co-speakers/{{a3}}
HTTP 200

DELETE {{base_url}}/talks/{{co_removed}}/co-speakers/{{a3}}
HTTP 200

# R3: a workshop in S4; a withdrawn talk stays; a co-speaker added and one removed after the talk was put there
GET {{base_url}}/programme
HTTP 200
[Asserts]
header "Content-Type" == "application/json;charset=UTF-8"
jsonpath "$" count == 4
jsonpath "$[0].slot" == "S2"
jsonpath "$[0].start" == "10:15"
jsonpath "$[0].end" == "11:15"
jsonpath "$[0].room" == "R1"
jsonpath "$[0].talkId" == {{co_added}}
jsonpath "$[0].title" == "Legacy is a feeling"
jsonpath "$[0].format" == "PRESENTATION"
jsonpath "$[0].speakers" count == 2
jsonpath "$[0].speakers[0]" == "Grace Hopper"
jsonpath "$[0].speakers[1]" == "Ada Lovelace"
jsonpath "$[1].slot" == "S3"
jsonpath "$[1].start" == "11:30"
jsonpath "$[1].end" == "12:30"
jsonpath "$[1].room" == "R1"
jsonpath "$[1].talkId" == {{co_removed}}
jsonpath "$[1].title" == "Legacy is a feeling"
jsonpath "$[1].format" == "PRESENTATION"
jsonpath "$[1].speakers" count == 1
jsonpath "$[1].speakers[0]" == "Grace Hopper"
jsonpath "$[2].slot" == "S4"
jsonpath "$[2].start" == "13:30"
jsonpath "$[2].end" == "15:30"
jsonpath "$[2].room" == "R2"
jsonpath "$[2].talkId" == {{hands_on}}
jsonpath "$[2].title" == "Hands on"
jsonpath "$[2].format" == "WORKSHOP"
jsonpath "$[2].speakers" count == 1
jsonpath "$[2].speakers[0]" == "Grace Hopper"
jsonpath "$[3].slot" == "S5"
jsonpath "$[3].start" == "15:45"
jsonpath "$[3].end" == "16:45"
jsonpath "$[3].room" == "R3"
jsonpath "$[3].talkId" == {{to_withdraw}}
jsonpath "$[3].title" == "Legacy is a feeling"
jsonpath "$[3].format" == "PRESENTATION"
jsonpath "$[3].speakers" count == 1
jsonpath "$[3].speakers[0]" == "Grace Hopper"

# R3, R4: the same rows in the CSV
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS2,10:15,11:15,R1,{{co_added}},Legacy is a feeling,PRESENTATION,Grace Hopper; Ada Lovelace\nS3,11:30,12:30,R1,{{co_removed}},Legacy is a feeling,PRESENTATION,Grace Hopper\nS4,13:30,15:30,R2,{{hands_on}},Hands on,WORKSHOP,Grace Hopper\nS5,15:45,16:45,R3,{{to_withdraw}},Legacy is a feeling,PRESENTATION,Grace Hopper\n"

DELETE {{base_url}}/programme/R2/S4
HTTP 204

DELETE {{base_url}}/programme/R3/S5
HTTP 204

DELETE {{base_url}}/programme/R1/S2
HTTP 204

DELETE {{base_url}}/programme/R1/S3
HTTP 204

# ================================================================ R4: the speakers, the main speaker first

POST {{base_url}}/speakers
{"name": "Grace Hopper", "email": "hopper-{{newUuid}}@example.com"}
HTTP 201
[Captures]
g4: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "lovelace-{{newUuid}}@example.com"}
HTTP 201
[Captures]
a4: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Barbara Liskov", "email": "liskov-{{newUuid}}@example.com"}
HTTP 201
[Captures]
b4: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Grace Hopper", "email": "hopper-{{newUuid}}@example.com"}
HTTP 201
[Captures]
g4b: jsonpath "$.id"

# Setup: a second "Grace Hopper" owns this talk, so the first stays under the limit of 3 when she co-speaks (submission-cap-cr)
POST {{base_url}}/talks
{"title": "Legacy is a feeling", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{g4b}}}
HTTP 201
[Captures]
alone: jsonpath "$.id"

PUT {{base_url}}/talks/{{alone}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{alone}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{alone}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Legacy is a feeling", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{g4}}}
HTTP 201
[Captures]
with_ada: jsonpath "$.id"

PUT {{base_url}}/talks/{{with_ada}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{with_ada}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{with_ada}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Legacy is a feeling", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{g4}}}
HTTP 201
[Captures]
with_two: jsonpath "$.id"

PUT {{base_url}}/talks/{{with_two}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{with_two}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{with_two}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Legacy is a feeling", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{a4}}}
HTTP 201
[Captures]
ada_main: jsonpath "$.id"

PUT {{base_url}}/talks/{{ada_main}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{ada_main}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{ada_main}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/talks/{{with_ada}}/co-speakers/{{a4}}
HTTP 200

PUT {{base_url}}/talks/{{with_two}}/co-speakers/{{b4}}
HTTP 200

PUT {{base_url}}/talks/{{with_two}}/co-speakers/{{a4}}
HTTP 200

PUT {{base_url}}/talks/{{ada_main}}/co-speakers/{{g4}}
HTTP 200

# Setup: empty the whole programme (204 if a talk was there, 404 if not)
DELETE {{base_url}}/programme/R1/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S3
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S3
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

DELETE {{base_url}}/programme/R2/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

PUT {{base_url}}/programme/R1/S1
{"talkId": {{alone}}}
HTTP 200

PUT {{base_url}}/programme/R1/S2
{"talkId": {{with_ada}}}
HTTP 200

PUT {{base_url}}/programme/R1/S3
{"talkId": {{with_two}}}
HTTP 200

PUT {{base_url}}/programme/R1/S5
{"talkId": {{ada_main}}}
HTTP 200

# R4: the main speaker, then the co-speakers in the order they were added
GET {{base_url}}/programme
HTTP 200
[Asserts]
header "Content-Type" == "application/json;charset=UTF-8"
jsonpath "$" count == 4
jsonpath "$[0].slot" == "S1"
jsonpath "$[0].start" == "09:00"
jsonpath "$[0].end" == "10:00"
jsonpath "$[0].room" == "R1"
jsonpath "$[0].talkId" == {{alone}}
jsonpath "$[0].title" == "Legacy is a feeling"
jsonpath "$[0].format" == "PRESENTATION"
jsonpath "$[0].speakers" count == 1
jsonpath "$[0].speakers[0]" == "Grace Hopper"
jsonpath "$[1].slot" == "S2"
jsonpath "$[1].start" == "10:15"
jsonpath "$[1].end" == "11:15"
jsonpath "$[1].room" == "R1"
jsonpath "$[1].talkId" == {{with_ada}}
jsonpath "$[1].title" == "Legacy is a feeling"
jsonpath "$[1].format" == "PRESENTATION"
jsonpath "$[1].speakers" count == 2
jsonpath "$[1].speakers[0]" == "Grace Hopper"
jsonpath "$[1].speakers[1]" == "Ada Lovelace"
jsonpath "$[2].slot" == "S3"
jsonpath "$[2].start" == "11:30"
jsonpath "$[2].end" == "12:30"
jsonpath "$[2].room" == "R1"
jsonpath "$[2].talkId" == {{with_two}}
jsonpath "$[2].title" == "Legacy is a feeling"
jsonpath "$[2].format" == "PRESENTATION"
jsonpath "$[2].speakers" count == 3
jsonpath "$[2].speakers[0]" == "Grace Hopper"
jsonpath "$[2].speakers[1]" == "Barbara Liskov"
jsonpath "$[2].speakers[2]" == "Ada Lovelace"
jsonpath "$[3].slot" == "S5"
jsonpath "$[3].start" == "15:45"
jsonpath "$[3].end" == "16:45"
jsonpath "$[3].room" == "R1"
jsonpath "$[3].talkId" == {{ada_main}}
jsonpath "$[3].title" == "Legacy is a feeling"
jsonpath "$[3].format" == "PRESENTATION"
jsonpath "$[3].speakers" count == 2
jsonpath "$[3].speakers[0]" == "Ada Lovelace"
jsonpath "$[3].speakers[1]" == "Grace Hopper"

# R4: one field, separated by a semicolon and a space
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{alone}},Legacy is a feeling,PRESENTATION,Grace Hopper\nS2,10:15,11:15,R1,{{with_ada}},Legacy is a feeling,PRESENTATION,Grace Hopper; Ada Lovelace\nS3,11:30,12:30,R1,{{with_two}},Legacy is a feeling,PRESENTATION,Grace Hopper; Barbara Liskov; Ada Lovelace\nS5,15:45,16:45,R1,{{ada_main}},Legacy is a feeling,PRESENTATION,Ada Lovelace; Grace Hopper\n"

DELETE {{base_url}}/programme/R1/S1
HTTP 204

DELETE {{base_url}}/programme/R1/S2
HTTP 204

DELETE {{base_url}}/programme/R1/S3
HTTP 204

DELETE {{base_url}}/programme/R1/S5
HTTP 204

# ================================================================ R5: the CSV is text/csv and UTF-8, with the exact header

POST {{base_url}}/speakers
{"name": "Grace Hopper", "email": "hopper-{{newUuid}}@example.com"}
HTTP 201
[Captures]
g5: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Kåre og Åse 🚀", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{g5}}}
HTTP 201
[Captures]
utf8: jsonpath "$.id"

PUT {{base_url}}/talks/{{utf8}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{utf8}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{utf8}}/status
{"status": "ACCEPTED"}
HTTP 200

# Setup: empty the whole programme (204 if a talk was there, 404 if not)
DELETE {{base_url}}/programme/R1/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S3
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S3
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

DELETE {{base_url}}/programme/R2/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

PUT {{base_url}}/programme/R1/S1
{"talkId": {{utf8}}}
HTTP 200

# R5: UTF-8 text, served as text/csv
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{utf8}},Kåre og Åse 🚀,PRESENTATION,Grace Hopper\n"

# R5: the file starts with the header, with no byte order mark, and the title is UTF-8 bytes
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
bytes startsWith hex,736c6f742c;
bytes contains hex,4bc3a57265206f6720c385736520f09f9a80;

# R5: a client that asks for JSON still gets the CSV
GET {{base_url}}/programme.csv
Accept: application/json
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{utf8}},Kåre og Åse 🚀,PRESENTATION,Grace Hopper\n"

# Our decisions: a client that asks for CSV on /programme still gets JSON
GET {{base_url}}/programme
Accept: text/csv
HTTP 200
[Asserts]
header "Content-Type" == "application/json;charset=UTF-8"
jsonpath "$" count == 1
jsonpath "$[0].talkId" == {{utf8}}

# Our decisions: query parameters are ignored
GET {{base_url}}/programme?room=R2
HTTP 200
[Asserts]
jsonpath "$" count == 1

DELETE {{base_url}}/programme/R1/S1
HTTP 204

# Our decisions: POST on /programme is 405, the views are read-only
POST {{base_url}}/programme
HTTP 405

# Our decisions: PUT on /programme is 405, the views are read-only
PUT {{base_url}}/programme
HTTP 405

# Our decisions: DELETE on /programme is 405, the views are read-only
DELETE {{base_url}}/programme
HTTP 405

# Our decisions: POST on /programme.csv is 405, the views are read-only
POST {{base_url}}/programme.csv
HTTP 405

# Our decisions: PUT on /programme.csv is 405, the views are read-only
PUT {{base_url}}/programme.csv
HTTP 405

# Our decisions: DELETE on /programme.csv is 405, the views are read-only
DELETE {{base_url}}/programme.csv
HTTP 405

# ================================================================ R6: line endings and quoting

POST {{base_url}}/speakers
{"name": "Grace Hopper", "email": "hopper-{{newUuid}}@example.com"}
HTTP 201
[Captures]
g6: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "lovelace-{{newUuid}}@example.com"}
HTTP 201
[Captures]
a6: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Legacy is a feeling", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{g6}}}
HTTP 201
[Captures]
le_a: jsonpath "$.id"

PUT {{base_url}}/talks/{{le_a}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{le_a}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{le_a}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Legacy is a feeling", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{a6}}}
HTTP 201
[Captures]
le_b: jsonpath "$.id"

PUT {{base_url}}/talks/{{le_b}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{le_b}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{le_b}}/status
{"status": "ACCEPTED"}
HTTP 200

# Setup: empty the whole programme (204 if a talk was there, 404 if not)
DELETE {{base_url}}/programme/R1/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S1
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S2
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S3
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S3
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

DELETE {{base_url}}/programme/R2/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S4
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R1/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R2/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

DELETE {{base_url}}/programme/R3/S5
HTTP *
[Asserts]
status toString matches /^(204|404)$/

PUT {{base_url}}/programme/R1/S1
{"talkId": {{le_a}}}
HTTP 200

PUT {{base_url}}/programme/R2/S1
{"talkId": {{le_b}}}
HTTP 200

# R6: Unix line endings, a newline after every line, no blank line at the end
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{le_a}},Legacy is a feeling,PRESENTATION,Grace Hopper\nS1,09:00,10:00,R2,{{le_b}},Legacy is a feeling,PRESENTATION,Ada Lovelace\n"

# R6: no carriage return anywhere
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
body not contains "\r"

DELETE {{base_url}}/programme/R1/S1
HTTP 204

DELETE {{base_url}}/programme/R2/S1
HTTP 204

POST {{base_url}}/speakers
{"name": "Grace Hopper", "email": "hopper-{{newUuid}}@example.com"}
HTTP 201
[Captures]
q1: jsonpath "$.id"

POST {{base_url}}/speakers
{"name": "Grace Hopper", "email": "hopper-{{newUuid}}@example.com"}
HTTP 201
[Captures]
q2: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Kotlin, really fast", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{q1}}}
HTTP 201
[Captures]
title_0: jsonpath "$.id"

PUT {{base_url}}/talks/{{title_0}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{title_0}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{title_0}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Kotlin, \"really\" fast", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{q1}}}
HTTP 201
[Captures]
title_1: jsonpath "$.id"

PUT {{base_url}}/talks/{{title_1}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{title_1}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{title_1}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Say \"hi\"", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{q1}}}
HTTP 201
[Captures]
title_2: jsonpath "$.id"

PUT {{base_url}}/talks/{{title_2}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{title_2}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{title_2}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "Two\nlines", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{q2}}}
HTTP 201
[Captures]
title_3: jsonpath "$.id"

PUT {{base_url}}/talks/{{title_3}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{title_3}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{title_3}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "=SUM(1;2)", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{q2}}}
HTTP 201
[Captures]
title_4: jsonpath "$.id"

PUT {{base_url}}/talks/{{title_4}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{title_4}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{title_4}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/talks
{"title": "  Spaces around  ", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{q2}}}
HTTP 201
[Captures]
title_5: jsonpath "$.id"

PUT {{base_url}}/talks/{{title_5}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{title_5}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{title_5}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/speakers
{"name": "Hopper, Grace", "email": "hopper-{{newUuid}}@example.com"}
HTTP 201
[Captures]
named_0: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Legacy is a feeling", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{named_0}}}
HTTP 201
[Captures]
name_0: jsonpath "$.id"

PUT {{base_url}}/talks/{{name_0}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{name_0}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{name_0}}/status
{"status": "ACCEPTED"}
HTTP 200

POST {{base_url}}/speakers
{"name": "Grace \"Amazing\" Hopper", "email": "amazing-{{newUuid}}@example.com"}
HTTP 201
[Captures]
named_1: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Legacy is a feeling", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{named_1}}}
HTTP 201
[Captures]
name_1: jsonpath "$.id"

PUT {{base_url}}/talks/{{name_1}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{name_1}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{name_1}}/status
{"status": "ACCEPTED"}
HTTP 200

PUT {{base_url}}/programme/R1/S1
{"talkId": {{title_0}}}
HTTP 200

# R6: the title 'Kotlin, really fast' as the field `"Kotlin, really fast"`
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{title_0}},\"Kotlin, really fast\",PRESENTATION,Grace Hopper\n"

DELETE {{base_url}}/programme/R1/S1
HTTP 204

PUT {{base_url}}/programme/R1/S1
{"talkId": {{title_1}}}
HTTP 200

# R6: the title 'Kotlin, "really" fast' as the field `"Kotlin, ""really"" fast"`
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{title_1}},\"Kotlin, \"\"really\"\" fast\",PRESENTATION,Grace Hopper\n"

DELETE {{base_url}}/programme/R1/S1
HTTP 204

PUT {{base_url}}/programme/R1/S1
{"talkId": {{title_2}}}
HTTP 200

# R6: the title 'Say "hi"' as the field `"Say ""hi"""`
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{title_2}},\"Say \"\"hi\"\"\",PRESENTATION,Grace Hopper\n"

DELETE {{base_url}}/programme/R1/S1
HTTP 204

PUT {{base_url}}/programme/R1/S1
{"talkId": {{title_3}}}
HTTP 200

# R6: the title 'Two⏎lines' as the field `"Two⏎lines"`
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{title_3}},\"Two\nlines\",PRESENTATION,Grace Hopper\n"

DELETE {{base_url}}/programme/R1/S1
HTTP 204

PUT {{base_url}}/programme/R1/S1
{"talkId": {{title_4}}}
HTTP 200

# R6: the title '=SUM(1;2)' as the field `=SUM(1;2)`
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{title_4}},=SUM(1;2),PRESENTATION,Grace Hopper\n"

DELETE {{base_url}}/programme/R1/S1
HTTP 204

PUT {{base_url}}/programme/R1/S1
{"talkId": {{title_5}}}
HTTP 200

# R6: the title '  Spaces around  ' as the field `  Spaces around  `
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{title_5}},  Spaces around  ,PRESENTATION,Grace Hopper\n"

DELETE {{base_url}}/programme/R1/S1
HTTP 204

PUT {{base_url}}/programme/R1/S1
{"talkId": {{name_0}}}
HTTP 200

# R6: the speaker 'Hopper, Grace' as the field `"Hopper, Grace"`
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{name_0}},Legacy is a feeling,PRESENTATION,\"Hopper, Grace\"\n"

DELETE {{base_url}}/programme/R1/S1
HTTP 204

PUT {{base_url}}/programme/R1/S1
{"talkId": {{name_1}}}
HTTP 200

# R6: the speaker 'Grace "Amazing" Hopper' as the field `"Grace ""Amazing"" Hopper"`
GET {{base_url}}/programme.csv
HTTP 200
[Asserts]
header "Content-Type" == "text/csv;charset=UTF-8"
body == "slot,start,end,room,talkId,title,format,speakers\nS1,09:00,10:00,R1,{{name_1}},Legacy is a feeling,PRESENTATION,\"Grace \"\"Amazing\"\" Hopper\"\n"

DELETE {{base_url}}/programme/R1/S1
HTTP 204
