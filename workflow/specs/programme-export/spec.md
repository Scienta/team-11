# Programme export (`programme-export`)

Source: `request.md` in this folder (the feature request from the harness, not edited).

## Rules

| ID | Rule |
|----|------|
| R1 | Two read-only views show everything on the programme ([Scheduling](../scheduling/spec.md)): a JSON list for the website and a CSV file for the print shop. Both have one row per talk on the programme, nothing else, in the same order. |
| R2 | The programme reads like a day: the earliest start first, and in one slot by room `R1`, `R2`, `R3`. |
| R3 | Each row shows the slot, its start and end (as in Scheduling R1), the room, and the talk's ID, title, format and speakers as they are now. |
| R4 | The speakers are names: the main speaker first, then the co-speakers in the order they were added ([Co-speakers](../co-speakers/spec.md) R4). In the CSV they are one field, separated by a semicolon and a space (`Grace Hopper; Ada Lovelace`). |
| R5 | The CSV is UTF-8 and is served as `text/csv`, also to a client that asks for JSON. Its first line is exactly `slot,start,end,room,talkId,title,format,speakers`. |
| R6 | The CSV is standard CSV ([RFC 4180](https://www.rfc-editor.org/rfc/rfc4180)) with Unix line endings. Every line ends in a newline, and there is no blank line at the end. A field is quoted only when it has a comma, a quote or a line break in it, and a quote in it is doubled (`"Kotlin, ""really"" fast"`). Every other field is written as it is. |
| R7 | An empty programme is an empty JSON list, or a CSV with only the header line. |

## API

| Method | Path | Request body | Success status | Response body |
|--------|------|--------------|----------------|---------------|
| GET | `/programme` | | 200 | JSON array of ProgrammeRow, `Content-Type: application/json` |
| GET | `/programme.csv` | | 200 | CSV text, `Content-Type: text/csv;charset=UTF-8` |

A ProgrammeRow is `{slot, start, end, room, talkId, title, format, speakers}`, for example
`{"slot": "S4", "start": "13:30", "end": "15:30", "room": "R2", "talkId": 7, "title": "Legacy is a feeling", "format": "WORKSHOP", "speakers": ["Grace Hopper", "Ada Lovelace"]}`.
A CSV line has the same eight values in the same order:
`S4,13:30,15:30,R2,7,Legacy is a feeling,WORKSHOP,Grace Hopper; Ada Lovelace`.

## Our decisions

| Case | Decision | Why |
|------|----------|-----|
| `Content-Type` of the CSV | `text/csv;charset=UTF-8` | R5: the media type is `text/csv`, and the charset tells the client that the file is UTF-8. Same as `/admin/talks.csv`. |
| `Accept` header on either path, for example `Accept: application/json` on `/programme.csv` or `Accept: text/csv` on `/programme` | Ignored: `/programme.csv` always answers 200 `text/csv`, and `/programme` always answers 200 `application/json`. Never 406. | R5 says so for the CSV. The path chooses the view, so the JSON path does the same. |
| Byte order mark | None | R5: the file starts with exactly the header line. |
| A talk on the programme that is no longer `ACCEPTED`, for example withdrawn | In both views, as it is now. There is no status field. | R1: "everything on the programme". Scheduling R7 keeps the entry until the committee takes the talk out. The request lists the fields of a row, and a status is not one of them. |
| Fields of a JSON row | Exactly the eight fields of ProgrammeRow, in that order | The request's schema. |
| A title that starts like a spreadsheet formula (`=`, `+`, `-`, `@`) | Written as it is, with no `'` in front | R6: "otherwise written as they are". The file is for the print shop's tooling, not for a spreadsheet. `/admin/talks.csv` keeps its formula protection (security review S6), because that file is for spreadsheets. For the security review: this is checklist item C7, decided by the request. |
| A line break in a title (`\n`, `\r` or `\r\n`) | The field is quoted, and the line break is written as it is stored | R6: RFC 4180 allows a line break in a quoted field. Only the line ends between rows must be `\n`. |
| Spaces at the start or end of a field | Written as they are, not quoted | R6: quote only for a comma, a quote or a line break. RFC 4180 keeps spaces as part of the field. |
| A speaker name with a comma, a quote, `; ` or a line break | The whole speakers field follows R6. The names are not escaped one by one. | R4 gives one field. In the JSON, the array keeps the names apart. Speaker names are not checked for these characters (Speakers R3). |
| Query parameters | Ignored | The request gives none. |
| Other methods on `/programme` and `/programme.csv` (POST, PUT, DELETE) | 405 | R1: read-only. Same as other paths without that method. |
| One moment in time | Each request reads the programme, the talks and the speakers in one read-only transaction | The rows of one answer never mix a programme from before a change with talks from after it. |
| An entry whose talk row is missing, or a speaker row that is missing | The entry, or the name, is left out | There are no foreign keys, and no endpoint deletes a talk or a speaker, so this happens only through direct changes to the database. An answer without the entry is better than a 500. |
| Who can read the export | Anyone, no authentication | Same as the other endpoints today (tech debt B11). The programme is public information for the website. |
| `Content-Disposition` | None | The request does not ask for a file name. |

## Examples

Grace Hopper, Ada Lovelace and Barbara Liskov are registered speakers. A talk is an `ACCEPTED`
`PRESENTATION` with Grace Hopper as main speaker and no co-speakers, unless the row says otherwise. A
row "A in `R1` `S1`" means that talk A was put there with `PUT /programme/R1/S1`. In the CSV
examples, `⏎` is the newline `\n`.

### R1, R7: one row per talk on the programme, in both views

| Programme | Other talks | JSON | CSV lines after the header |
|-----------|-------------|------|----------------------------|
| A in `R1` `S1`, B in `R2` `S3` | | 2 rows: A, then B | 2 lines: A, then B |
| empty | | `[]` | none: the body is `slot,start,end,room,talkId,title,format,speakers⏎` |
| empty | C accepted, D submitted, E taken out of `R1` `S2` again | `[]` | none |
| A in `R1` `S1` | C accepted, not on the programme | 1 row: A | 1 line: A |

### R2: the order of a day

| Put on the programme in this order | Order in the JSON and in the CSV |
|------------------------------------|----------------------------------|
| `R3` `S1`, `R1` `S2`, `R1` `S1`, `R2` `S1` | `R1` `S1`, `R2` `S1`, `R3` `S1`, `R1` `S2` |
| `R1` `S5`, `R3` `S4`, `R2` `S4` | `R2` `S4`, `R3` `S4`, `R1` `S5` |

### R3: what a row shows

| Talk | Place | Change after it was put on the programme | JSON row | CSV line |
|------|-------|------------------------------------------|----------|----------|
| A, "Legacy is a feeling" | `R1` `S1` | | `{"slot": "S1", "start": "09:00", "end": "10:00", "room": "R1", "talkId": A, "title": "Legacy is a feeling", "format": "PRESENTATION", "speakers": ["Grace Hopper"]}` | `S1,09:00,10:00,R1,A,Legacy is a feeling,PRESENTATION,Grace Hopper` |
| A, "Hands on", `WORKSHOP` | `R2` `S4` | | `{"slot": "S4", "start": "13:30", "end": "15:30", "room": "R2", "talkId": A, "title": "Hands on", "format": "WORKSHOP", "speakers": ["Grace Hopper"]}` | `S4,13:30,15:30,R2,A,Hands on,WORKSHOP,Grace Hopper` |
| A, "Legacy is a feeling" | `R3` `S5` | Grace Hopper withdraws A | the same row as before the change, with `S5`, `15:45`, `16:45`, `R3` | `S5,15:45,16:45,R3,A,Legacy is a feeling,PRESENTATION,Grace Hopper` |
| A, "Legacy is a feeling" | `R1` `S2` | Ada Lovelace becomes co-speaker of A | `"speakers": ["Grace Hopper", "Ada Lovelace"]` | `S2,10:15,11:15,R1,A,Legacy is a feeling,PRESENTATION,Grace Hopper; Ada Lovelace` |
| A with co-speaker Ada Lovelace | `R1` `S3` | Ada Lovelace is removed as co-speaker | `"speakers": ["Grace Hopper"]` | `S3,11:30,12:30,R1,A,Legacy is a feeling,PRESENTATION,Grace Hopper` |

### R4: the speakers

| Main speaker | Co-speakers, in the order added | JSON `speakers` | CSV field |
|--------------|---------------------------------|-----------------|-----------|
| Grace Hopper | none | `["Grace Hopper"]` | `Grace Hopper` |
| Grace Hopper | Ada Lovelace | `["Grace Hopper", "Ada Lovelace"]` | `Grace Hopper; Ada Lovelace` |
| Grace Hopper | Barbara Liskov, then Ada Lovelace | `["Grace Hopper", "Barbara Liskov", "Ada Lovelace"]` | `Grace Hopper; Barbara Liskov; Ada Lovelace` |
| Ada Lovelace | Grace Hopper | `["Ada Lovelace", "Grace Hopper"]` | `Ada Lovelace; Grace Hopper` |

### R5: the CSV is `text/csv` and UTF-8, with the exact header

| Request | Status | `Content-Type` | Body |
|---------|--------|----------------|------|
| `GET /programme.csv` | 200 | `text/csv;charset=UTF-8` | starts with `slot,start,end,room,talkId,title,format,speakers⏎` |
| `GET /programme.csv` with `Accept: application/json` | 200 | `text/csv;charset=UTF-8` | the same body |
| `GET /programme` with `Accept: text/csv` | 200 | `application/json` | the JSON list |
| `GET /programme.csv`, A "Kåre og Åse 🚀" in `R1` `S1` | 200 | `text/csv;charset=UTF-8` | the line has `Kåre og Åse 🚀` as UTF-8 bytes, and no byte order mark is in front of the header |

### R6: line endings and quoting

Line endings, with A in `R1` `S1` and B in `R2` `S1`: the whole body is exactly
`slot,start,end,room,talkId,title,format,speakers⏎S1,09:00,10:00,R1,A,Legacy is a feeling,PRESENTATION,Grace Hopper⏎S1,09:00,10:00,R2,B,Legacy is a feeling,PRESENTATION,Ada Lovelace⏎`.
There is no `\r` and no blank line at the end.

| Title of A | CSV `title` field |
|------------|-------------------|
| `Legacy is a feeling` | `Legacy is a feeling` |
| `Kotlin, really fast` | `"Kotlin, really fast"` |
| `Kotlin, "really" fast` | `"Kotlin, ""really"" fast"` |
| `Say "hi"` | `"Say ""hi"""` |
| `Two` line break `lines` | `"Two⏎lines"`: the line break is inside the quotes |
| `=SUM(1;2)` | `=SUM(1;2)` (see "Our decisions") |
| `  Spaces around  ` | `  Spaces around  ` |

| Main speaker of A | CSV `speakers` field |
|-------------------|----------------------|
| `Hopper, Grace` | `"Hopper, Grace"` |
| `Grace "Amazing" Hopper` | `"Grace ""Amazing"" Hopper"` |

## Effect on existing features

- Scheduling: no change. `GET`, `PUT` and `DELETE /programme/{room}/{slot}` stay as they are. The
  new paths `/programme` and `/programme.csv` have no room and slot, so they do not collide with them.
- `/admin/talks.csv`: no change. It keeps its semicolons and its formula protection.
- Talks, co-speakers, acceptance: no change. The export only reads them.

## Open questions for the product owner

None. The request gives the business rules. The product owner's earlier answers do not touch the
export, and he asked us not to ask technical questions (Slack #team-11, 2026-11-06 13:11 and 13:14).
