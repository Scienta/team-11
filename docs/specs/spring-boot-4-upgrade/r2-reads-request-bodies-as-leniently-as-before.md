## R2: Reads request bodies as leniently as before

Unknown fields are ignored, comments are allowed (the mobile client sends them), and a
scalar of the wrong JSON type is coerced to the field's type. $email is a new address and
$speaker a registered speaker.


| Scenario | Path | Body | Status? |
| --- | --- | --- | --- |
| Unknown field in a speaker | /speakers | {"name":"Grace Hopper","email":"$email","twitter":"@grace"} | 201 |
| Comment in a speaker | /speakers | {/* from the app */ "name":"Grace Hopper","email":"$email"} | 201 |
| Unknown field in a talk | /talks | {"title":"Legacy","format":"PRESENTATION","level":"BEGINNER","speakerId":$speaker,"room":"A"} | 201 |
| Speaker ID as text | /talks | {"title":"Legacy","format":"PRESENTATION","level":"BEGINNER","speakerId":"$speaker"} | 201 |
| Title as a number | /talks | {"title":5,"format":"PRESENTATION","level":"BEGINNER","speakerId":$speaker} | 201 |
