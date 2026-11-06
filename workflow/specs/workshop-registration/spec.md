# Workshop registration with capacity (`workshop-registration`)

Source: `request.md` in this folder (the feature request from the harness, not edited).

## Rules

| ID | Rule |
|----|------|
| R1 | An attendee registers for a workshop with a name and an email. Both are required. |
| R2 | Only a talk with format `WORKSHOP` ([Talks](../talks/spec.md) R4) is a workshop. |
| R3 | Only an `ACCEPTED` workshop ([Acceptance](../acceptance/spec.md) R1) takes registrations. |
| R4 | When a workshop is withdrawn later, the attendees already registered stay on its list. |
| R5 | A workshop has room for 20 attendees, and never more, also when everybody registers at the same moment. |
| R6 | One registration per email per workshop, whatever the letter case: `ada@example.com` and `ADA@Example.com` are the same person. |
| R7 | The same person can register for several workshops. |
| R8 | The organisers see the attendees of a workshop, in the order they registered. |

## API

| Method | Path | Request body | Success status | Response body |
|--------|------|--------------|----------------|---------------|
| POST | `/workshops/{id}/registrations` | `{name, email}` | 201 | Registration |
| GET | `/workshops/{id}/registrations` | | 200 | list of Registration, oldest first |

`{id}` is the workshop's talk ID. A Registration is `{id, workshopId, name, email}`. `email` is the
email as the attendee typed it.

## Our decisions

| Case | Decision | Why |
|------|----------|-----|
| Name missing, empty or only spaces | 400 | R1. Same as the speaker's name (Speakers R3). |
| Email missing, empty, only spaces, or not a real address | 400 | R1. The same rule as for speakers (`something@something.domain`, no spaces), from one place in the code, as the PO asked for speakers on 2026-11-06. |
| Missing body or malformed JSON | 400 | Same as the base features |
| ID in the path that is not a number | 400 | Same as the base features |
| Unknown talk in the path, on POST and GET | 404 | Same as the base features |
| POST with an unusable body to an unknown talk | 400 | The body is checked before the workshop is looked up (`WorkshopRegistrationController`), as in the base features. A request that is wrong in itself is a bad request, whatever it is for. |
| A talk that is not a `WORKSHOP` (R2), on POST and GET | 404 | The path names a workshop. A talk of another format is not a workshop, so the resource does not exist. |
| A workshop that is `SUBMITTED`, `WAITLISTED`, `REJECTED` or `WITHDRAWN` (R3) | 409 on POST, nothing is saved | A conflict with the current state, as in Acceptance R8 (rating a closed talk). |
| GET for a workshop that is not `ACCEPTED` | 200 with its list | R4 needs the list of a withdrawn workshop. A workshop that was never accepted has an empty list. |
| The 21st registration (R5) | 409, nothing is saved | A conflict with the current state, as for the limit of 3 talks (Talks R7). |
| The same email again, in any letter case (R6) | 409, nothing is saved | Same as the speakers' email (Speakers R5). |
| Email stored and shown | As typed. Compared in lower case. | The API says "as the attendee typed it". |
| The attendee is not a registered speaker | Allowed | Attendees are not speakers. The request asks for a name and an email only. |
| Order of the list (R8) | By registration ID, oldest first | IDs grow in the order the registrations are saved. |
| Who can read the list | Anyone, no authentication | Same as the other endpoints today (tech debt B11). |
| Concurrency (R5, R6) | A registration locks the talk row (`SELECT … FOR UPDATE`), counts, and inserts in one transaction. A unique index on the workshop and the lower-case email backs R6. | Advisory locks are not available on the platform. A plain conditional `INSERT` lets two requests both see 19 seats taken. |

## Examples

### R1: a name and a real email

| Name | Email | Status |
|------|-------|--------|
| `Ada Lovelace` | `ada@example.com` | 201 |
| empty | `ada@example.com` | 400 |
| only spaces | `ada@example.com` | 400 |
| missing | `ada@example.com` | 400 |
| `Ada Lovelace` | `not-an-email` | 400 |
| `Ada Lovelace` | empty | 400 |
| `Ada Lovelace` | missing | 400 |

### R2: only a workshop

| Talk format | Status of POST | Status of GET |
|-------------|----------------|---------------|
| `WORKSHOP` (accepted) | 201 | 200 |
| `LIGHTNING_10`, `LIGHTNING_20` or `PRESENTATION` (accepted) | 404 | 404 |
| no talk with this ID | 404 | 404 |

### R3: only an accepted workshop

| Workshop status | Status of POST |
|-----------------|----------------|
| `ACCEPTED` | 201 |
| `SUBMITTED`, `WAITLISTED`, `REJECTED` or `WITHDRAWN` | 409 |

### R4: a withdrawn workshop keeps its attendees

| Given | Request | Status | Response |
|-------|---------|--------|----------|
| an accepted workshop with Ada and Grace registered, then withdrawn | GET registrations | 200 | Ada, Grace |
| the same workshop | POST a registration for Barbara | 409 | |

### R5: 20 seats

| Already registered | Request | Status |
|--------------------|---------|--------|
| 19 | one registration | 201 |
| 20 | one registration | 409 |
| 0 | 30 registrations with different emails at the same time | twenty 201, ten 409; the list has 20 attendees |

### R6: one registration per email

| Registered email | New email | Status |
|------------------|-----------|--------|
| `ada@example.com` | `ada@example.com` or `ADA@Example.com` | 409 |
| `ada@example.com` | `grace@example.com` | 201 |
| none | 10 registrations with `ada@example.com` at the same time | one 201, nine 409 |

### R7: several workshops

| Given | Request | Status |
|-------|---------|--------|
| Ada registered for workshop 1 | Ada registers for workshop 2 | 201 |

### R8: the list in registration order

| Registered, in this order | GET registrations returns |
|---------------------------|---------------------------|
| Ada, Grace, Barbara | Ada, Grace, Barbara |
| `ADA@Example.com` | `ADA@Example.com`, as typed |
| nobody | `[]` |

## Effect on existing features

- Talks and Acceptance: no change. A registration reads the talk's format and status and changes
  neither. A withdrawal does not delete registrations (R4).
- `/admin/stats` and the CSV export: no change. Registrations are not part of them. The PO's answer
  about withdrawn talks in the statistics (a separate line) is not about registrations.

## Open questions for the product owner

- [ ] Do the workshop's own speakers take seats out of the 20? We assume no: the 20 seats are for
  attendees only.
- [ ] Can an attendee cancel a registration and free the seat? The request has no endpoint for it,
  so we build none. Is that right for now?
