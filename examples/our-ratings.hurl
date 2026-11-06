# Our examples for Ratings (workflow/specs/ratings/spec.md), beyond the published happy path.
# Run: examples/run.sh [base-url]

POST {{base_url}}/speakers
{"name": "Barbara Liskov", "email": "barbara-{{newUuid}}@example.com"}
HTTP 201
[Captures]
speaker_id: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "Data abstraction", "format": "PRESENTATION", "level": "ADVANCED", "speakerId": {{speaker_id}}}
HTTP 201
[Captures]
talk_id: jsonpath "$.id"

# R1: reviewer 1
PUT {{base_url}}/talks/{{talk_id}}/ratings/1
{"score": 4}
HTTP 200

# R1: reviewer 2147483647
PUT {{base_url}}/talks/{{talk_id}}/ratings/2147483647
{"score": 4}
HTTP 200

# R1: reviewer 2147483648
PUT {{base_url}}/talks/{{talk_id}}/ratings/2147483648
{"score": 4}
HTTP 200

# R1: reviewer 9223372036854775807
PUT {{base_url}}/talks/{{talk_id}}/ratings/9223372036854775807
{"score": 4}
HTTP 200

# R1: reviewer 9223372036854775808
PUT {{base_url}}/talks/{{talk_id}}/ratings/9223372036854775808
{"score": 4}
HTTP 400

# R1: reviewer 0
PUT {{base_url}}/talks/{{talk_id}}/ratings/0
{"score": 4}
HTTP 400

# R1: reviewer -1
PUT {{base_url}}/talks/{{talk_id}}/ratings/-1
{"score": 4}
HTTP 400

# R4: 4, 4, 4, 5 averages 4.25, shown as 4.3 (halves round up)
POST {{base_url}}/talks
{"title": "Abstraction mechanisms in CLU", "format": "WORKSHOP", "level": "ADVANCED", "speakerId": {{speaker_id}}}
HTTP 201
[Captures]
average_talk: jsonpath "$.id"

GET {{base_url}}/talks/{{average_talk}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == null
jsonpath "$.ratingCount" == 0

PUT {{base_url}}/talks/{{average_talk}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{average_talk}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{average_talk}}/ratings/3
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{average_talk}}/ratings/4
{"score": 5}
HTTP 200

GET {{base_url}}/talks/{{average_talk}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 4.3
jsonpath "$.ratingCount" == 4
