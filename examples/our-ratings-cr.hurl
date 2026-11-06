# Our examples for the ratings change request (workflow/specs/ratings-cr/spec.md).
# Each score comes from a different reviewer unless a comment says otherwise; 4x39 means 39 reviewers
# who each gave 4. Generated from the spec's example tables.
# Run: examples/run.sh [base-url]

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp1: jsonpath "$.id"

# R6: no ratings -> null, 0 ratings
POST {{base_url}}/talks
{"title": "R6 null", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp1}}}
HTTP 201
[Captures]
talk2: jsonpath "$.id"

GET {{base_url}}/talks/{{talk2}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == null
jsonpath "$.ratingCount" == 0

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp3: jsonpath "$.id"

# R1: 5 -> 5.0, 1 ratings
POST {{base_url}}/talks
{"title": "R1 5.0", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp3}}}
HTTP 201
[Captures]
talk4: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk4}}/ratings/1
{"score": 5}
HTTP 200

GET {{base_url}}/talks/{{talk4}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 5.0
jsonpath "$.ratingCount" == 1

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp5: jsonpath "$.id"

# R1: 4, 5 -> 4.5, 2 ratings
POST {{base_url}}/talks
{"title": "R1 4.5", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp5}}}
HTTP 201
[Captures]
talk6: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk6}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk6}}/ratings/2
{"score": 5}
HTTP 200

GET {{base_url}}/talks/{{talk6}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 4.5
jsonpath "$.ratingCount" == 2

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp7: jsonpath "$.id"

# R1: 1, 1, 2 -> 1.33, 3 ratings
POST {{base_url}}/talks
{"title": "R1 1.33", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp7}}}
HTTP 201
[Captures]
talk8: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk8}}/ratings/1
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk8}}/ratings/2
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk8}}/ratings/3
{"score": 2}
HTTP 200

GET {{base_url}}/talks/{{talk8}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 1.33
jsonpath "$.ratingCount" == 3

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp9: jsonpath "$.id"

# R1: 1, 2, 2 -> 1.67, 3 ratings
POST {{base_url}}/talks
{"title": "R1 1.67", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp9}}}
HTTP 201
[Captures]
talk10: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk10}}/ratings/1
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk10}}/ratings/2
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk10}}/ratings/3
{"score": 2}
HTTP 200

GET {{base_url}}/talks/{{talk10}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 1.67
jsonpath "$.ratingCount" == 3

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp11: jsonpath "$.id"

# R1: 4, 4, 4, 5 -> 4.25, 4 ratings (was 4.3)
POST {{base_url}}/talks
{"title": "R1 4.25", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp11}}}
HTTP 201
[Captures]
talk12: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk12}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk12}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk12}}/ratings/3
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk12}}/ratings/4
{"score": 5}
HTTP 200

GET {{base_url}}/talks/{{talk12}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 4.25
jsonpath "$.ratingCount" == 4

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp13: jsonpath "$.id"

# R1: 1, 5, 5, 5 -> 4.0, 4 ratings (no trim at 4)
POST {{base_url}}/talks
{"title": "R1 4.0", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp13}}}
HTTP 201
[Captures]
talk14: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk14}}/ratings/1
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk14}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk14}}/ratings/3
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk14}}/ratings/4
{"score": 5}
HTTP 200

GET {{base_url}}/talks/{{talk14}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 4.0
jsonpath "$.ratingCount" == 4

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp15: jsonpath "$.id"

# R2: 1, 2, 3, 4, 5 -> 3.0, 5 ratings
POST {{base_url}}/talks
{"title": "R2 3.0", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp15}}}
HTTP 201
[Captures]
talk16: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk16}}/ratings/1
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk16}}/ratings/2
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk16}}/ratings/3
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk16}}/ratings/4
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk16}}/ratings/5
{"score": 5}
HTTP 200

GET {{base_url}}/talks/{{talk16}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 3.0
jsonpath "$.ratingCount" == 5

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp17: jsonpath "$.id"

# R2: 5, 5, 5, 1, 1 -> 3.67, 5 ratings (plain mean 3.4)
POST {{base_url}}/talks
{"title": "R2 3.67", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp17}}}
HTTP 201
[Captures]
talk18: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk18}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk18}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk18}}/ratings/3
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk18}}/ratings/4
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk18}}/ratings/5
{"score": 1}
HTTP 200

GET {{base_url}}/talks/{{talk18}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 3.67
jsonpath "$.ratingCount" == 5

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp19: jsonpath "$.id"

# R2: 1, 5, 5, 5, 5 -> 5.0, 5 ratings
POST {{base_url}}/talks
{"title": "R2 5.0", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp19}}}
HTTP 201
[Captures]
talk20: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk20}}/ratings/1
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk20}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk20}}/ratings/3
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk20}}/ratings/4
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk20}}/ratings/5
{"score": 5}
HTTP 200

GET {{base_url}}/talks/{{talk20}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 5.0
jsonpath "$.ratingCount" == 5

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp21: jsonpath "$.id"

# R2: 5, 5, 5, 5, 5 -> 5.0, 5 ratings
POST {{base_url}}/talks
{"title": "R2 5.0", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp21}}}
HTTP 201
[Captures]
talk22: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk22}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk22}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk22}}/ratings/3
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk22}}/ratings/4
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk22}}/ratings/5
{"score": 5}
HTTP 200

GET {{base_url}}/talks/{{talk22}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 5.0
jsonpath "$.ratingCount" == 5

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp23: jsonpath "$.id"

# R2: 1, 1, 1, 1, 5 -> 1.0, 5 ratings
POST {{base_url}}/talks
{"title": "R2 1.0", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp23}}}
HTTP 201
[Captures]
talk24: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk24}}/ratings/1
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk24}}/ratings/2
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk24}}/ratings/3
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk24}}/ratings/4
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk24}}/ratings/5
{"score": 5}
HTTP 200

GET {{base_url}}/talks/{{talk24}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 1.0
jsonpath "$.ratingCount" == 5

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp25: jsonpath "$.id"

# R2: 1, 2, 2, 3, 4, 5 -> 2.75, 6 ratings
POST {{base_url}}/talks
{"title": "R2 2.75", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp25}}}
HTTP 201
[Captures]
talk26: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk26}}/ratings/1
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk26}}/ratings/2
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk26}}/ratings/3
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk26}}/ratings/4
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk26}}/ratings/5
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk26}}/ratings/6
{"score": 5}
HTTP 200

GET {{base_url}}/talks/{{talk26}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 2.75
jsonpath "$.ratingCount" == 6

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp27: jsonpath "$.id"

# R3: 5, 4, 3x7, 1 -> 3.12, 10 ratings (25 / 8 = 3.125)
POST {{base_url}}/talks
{"title": "R3 3.12", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp27}}}
HTTP 201
[Captures]
talk28: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk28}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk28}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk28}}/ratings/3
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk28}}/ratings/4
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk28}}/ratings/5
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk28}}/ratings/6
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk28}}/ratings/7
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk28}}/ratings/8
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk28}}/ratings/9
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk28}}/ratings/10
{"score": 1}
HTTP 200

GET {{base_url}}/talks/{{talk28}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 3.12
jsonpath "$.ratingCount" == 10

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp29: jsonpath "$.id"

# R3: 5, 4x3, 3x5, 1 -> 3.38, 10 ratings (27 / 8 = 3.375)
POST {{base_url}}/talks
{"title": "R3 3.38", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp29}}}
HTTP 201
[Captures]
talk30: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk30}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk30}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk30}}/ratings/3
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk30}}/ratings/4
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk30}}/ratings/5
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk30}}/ratings/6
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk30}}/ratings/7
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk30}}/ratings/8
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk30}}/ratings/9
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk30}}/ratings/10
{"score": 1}
HTTP 200

GET {{base_url}}/talks/{{talk30}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 3.38
jsonpath "$.ratingCount" == 10

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp31: jsonpath "$.id"

# R3: 5x2, 4x39, 1 -> 4.02, 42 ratings (161 / 40 = 4.025, a double gives 4.03); R4: all 42 count
POST {{base_url}}/talks
{"title": "R3 4.02", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp31}}}
HTTP 201
[Captures]
talk32: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk32}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/3
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/4
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/5
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/6
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/7
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/8
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/9
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/10
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/11
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/12
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/13
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/14
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/15
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/16
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/17
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/18
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/19
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/20
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/21
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/22
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/23
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/24
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/25
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/26
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/27
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/28
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/29
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/30
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/31
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/32
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/33
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/34
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/35
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/36
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/37
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/38
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/39
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/40
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/41
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk32}}/ratings/42
{"score": 1}
HTTP 200

GET {{base_url}}/talks/{{talk32}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 4.02
jsonpath "$.ratingCount" == 42

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp33: jsonpath "$.id"

# R3: 5x20, 4x21, 1 -> 4.48, 42 ratings (179 / 40 = 4.475, a double gives 4.47)
POST {{base_url}}/talks
{"title": "R3 4.48", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp33}}}
HTTP 201
[Captures]
talk34: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk34}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/3
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/4
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/5
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/6
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/7
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/8
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/9
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/10
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/11
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/12
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/13
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/14
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/15
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/16
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/17
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/18
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/19
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/20
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/21
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/22
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/23
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/24
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/25
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/26
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/27
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/28
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/29
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/30
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/31
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/32
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/33
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/34
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/35
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/36
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/37
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/38
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/39
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/40
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/41
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk34}}/ratings/42
{"score": 1}
HTTP 200

GET {{base_url}}/talks/{{talk34}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 4.48
jsonpath "$.ratingCount" == 42

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp35: jsonpath "$.id"

# R3: 5, 3x7, 2x33, 1 -> 2.18, 42 ratings (87 / 40 = 2.175, a double gives 2.17)
POST {{base_url}}/talks
{"title": "R3 2.18", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp35}}}
HTTP 201
[Captures]
talk36: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk36}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/2
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/3
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/4
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/5
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/6
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/7
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/8
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/9
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/10
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/11
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/12
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/13
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/14
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/15
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/16
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/17
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/18
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/19
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/20
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/21
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/22
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/23
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/24
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/25
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/26
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/27
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/28
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/29
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/30
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/31
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/32
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/33
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/34
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/35
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/36
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/37
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/38
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/39
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/40
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/41
{"score": 2}
HTTP 200

PUT {{base_url}}/talks/{{talk36}}/ratings/42
{"score": 1}
HTTP 200

GET {{base_url}}/talks/{{talk36}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 2.18
jsonpath "$.ratingCount" == 42

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp37: jsonpath "$.id"

# R1: reviewer 1 gives 1, then 5; reviewers 2-4 give 5, 5, 1: 4 ratings 5, 5, 5, 1, no trim -> 4.0
POST {{base_url}}/talks
{"title": "Rates again", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp37}}}
HTTP 201
[Captures]
talk38: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk38}}/ratings/1
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk38}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk38}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk38}}/ratings/3
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk38}}/ratings/4
{"score": 1}
HTTP 200

GET {{base_url}}/talks/{{talk38}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 4.0
jsonpath "$.ratingCount" == 4

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp39: jsonpath "$.id"

# R2: reviewer 1 gives 5, then 1; reviewers 2-5 give 1, 3, 4, 5: keeps 1, 3, 4 -> 2.67
POST {{base_url}}/talks
{"title": "Rates again", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp39}}}
HTTP 201
[Captures]
talk40: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk40}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk40}}/ratings/1
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk40}}/ratings/2
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk40}}/ratings/3
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk40}}/ratings/4
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk40}}/ratings/5
{"score": 5}
HTTP 200

GET {{base_url}}/talks/{{talk40}}
HTTP 200
[Asserts]
jsonpath "$.averageRating" == 2.67
jsonpath "$.ratingCount" == 5

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp41: jsonpath "$.id"

# R5: same rounded average 3.12 (A exactly 3.125, B 3.1176...), B has more ratings -> B, A, C
POST {{base_url}}/talks
{"title": "A", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp41}}}
HTTP 201
[Captures]
talk42: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk42}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk42}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk42}}/ratings/3
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk42}}/ratings/4
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk42}}/ratings/5
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk42}}/ratings/6
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk42}}/ratings/7
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk42}}/ratings/8
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk42}}/ratings/9
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk42}}/ratings/10
{"score": 1}
HTTP 200

POST {{base_url}}/talks
{"title": "B", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp41}}}
HTTP 201
[Captures]
talk43: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk43}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/3
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/4
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/5
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/6
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/7
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/8
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/9
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/10
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/11
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/12
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/13
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/14
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/15
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/16
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/17
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/18
{"score": 3}
HTTP 200

PUT {{base_url}}/talks/{{talk43}}/ratings/19
{"score": 1}
HTTP 200

POST {{base_url}}/talks
{"title": "C", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp41}}}
HTTP 201
[Captures]
talk44: jsonpath "$.id"

GET {{base_url}}/talks?speakerId={{sp41}}&sort=rating
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].title" == "B"
jsonpath "$[1].title" == "A"
jsonpath "$[2].title" == "C"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp45: jsonpath "$.id"

# R5: same average and count -> oldest first
POST {{base_url}}/talks
{"title": "A", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp45}}}
HTTP 201
[Captures]
talk46: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk46}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk46}}/ratings/2
{"score": 5}
HTTP 200

POST {{base_url}}/talks
{"title": "B", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp45}}}
HTTP 201
[Captures]
talk47: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk47}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk47}}/ratings/2
{"score": 5}
HTTP 200

POST {{base_url}}/talks
{"title": "C", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp45}}}
HTTP 201
[Captures]
talk48: jsonpath "$.id"

GET {{base_url}}/talks?speakerId={{sp45}}&sort=rating
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].title" == "A"
jsonpath "$[1].title" == "B"
jsonpath "$[2].title" == "C"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp49: jsonpath "$.id"

# R5: trimmed averages 3.67, 4.0, 5.0 -> C, B, A
POST {{base_url}}/talks
{"title": "A", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp49}}}
HTTP 201
[Captures]
talk50: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk50}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk50}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk50}}/ratings/3
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk50}}/ratings/4
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk50}}/ratings/5
{"score": 1}
HTTP 200

POST {{base_url}}/talks
{"title": "B", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp49}}}
HTTP 201
[Captures]
talk51: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk51}}/ratings/1
{"score": 1}
HTTP 200

PUT {{base_url}}/talks/{{talk51}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk51}}/ratings/3
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk51}}/ratings/4
{"score": 5}
HTTP 200

POST {{base_url}}/talks
{"title": "C", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp49}}}
HTTP 201
[Captures]
talk52: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk52}}/ratings/1
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk52}}/ratings/2
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk52}}/ratings/3
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk52}}/ratings/4
{"score": 5}
HTTP 200

PUT {{base_url}}/talks/{{talk52}}/ratings/5
{"score": 1}
HTTP 200

GET {{base_url}}/talks?speakerId={{sp49}}&sort=rating
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].title" == "C"
jsonpath "$[1].title" == "B"
jsonpath "$[2].title" == "A"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp53: jsonpath "$.id"

# R5: 4.25 and 4.33 (both 4.3 with one decimal) -> C, B, then the unrated A
POST {{base_url}}/talks
{"title": "A", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp53}}}
HTTP 201
[Captures]
talk54: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "B", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp53}}}
HTTP 201
[Captures]
talk55: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk55}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk55}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk55}}/ratings/3
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk55}}/ratings/4
{"score": 5}
HTTP 200

POST {{base_url}}/talks
{"title": "C", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp53}}}
HTTP 201
[Captures]
talk56: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk56}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk56}}/ratings/2
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk56}}/ratings/3
{"score": 5}
HTTP 200

GET {{base_url}}/talks?speakerId={{sp53}}&sort=rating
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].title" == "C"
jsonpath "$[1].title" == "B"
jsonpath "$[2].title" == "A"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp57: jsonpath "$.id"

# R5: unrated last, oldest first
POST {{base_url}}/talks
{"title": "A", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp57}}}
HTTP 201
[Captures]
talk58: jsonpath "$.id"

POST {{base_url}}/talks
{"title": "B", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp57}}}
HTTP 201
[Captures]
talk59: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk59}}/ratings/1
{"score": 1}
HTTP 200

POST {{base_url}}/talks
{"title": "C", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp57}}}
HTTP 201
[Captures]
talk60: jsonpath "$.id"

GET {{base_url}}/talks?speakerId={{sp57}}&sort=rating
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].title" == "B"
jsonpath "$[1].title" == "A"
jsonpath "$[2].title" == "C"

POST {{base_url}}/speakers
{"name": "Ada Lovelace", "email": "ada-{{newUuid}}@example.com"}
HTTP 201
[Captures]
sp61: jsonpath "$.id"

# R5: withdrawn after all others (Acceptance R9)
POST {{base_url}}/talks
{"title": "A", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp61}}}
HTTP 201
[Captures]
talk62: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk62}}/ratings/1
{"score": 4}
HTTP 200

PUT {{base_url}}/talks/{{talk62}}/ratings/2
{"score": 5}
HTTP 200

POST {{base_url}}/talks
{"title": "B", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp61}}}
HTTP 201
[Captures]
talk63: jsonpath "$.id"

PUT {{base_url}}/talks/{{talk63}}/ratings/1
{"score": 3}
HTTP 200

POST {{base_url}}/talks
{"title": "C", "format": "PRESENTATION", "level": "BEGINNER", "speakerId": {{sp61}}}
HTTP 201
[Captures]
talk64: jsonpath "$.id"

POST {{base_url}}/talks/{{talk62}}/withdraw
{"speakerId": {{sp61}}}
HTTP 200

GET {{base_url}}/talks?speakerId={{sp61}}&sort=rating
HTTP 200
[Asserts]
jsonpath "$" count == 3
jsonpath "$[0].title" == "B"
jsonpath "$[1].title" == "C"
jsonpath "$[2].title" == "A"
