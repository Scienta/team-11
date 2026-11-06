# Workshop registration with capacity (`workshop-registration`)

Source: `request.md` in this folder (the feature request from the harness, not edited).

## Rules

R1–R8 come from the feature request. R9 comes from the product owner's answer in Slack #team-11
on 2026-11-06 at 14:01. R10 is our answer to the product owner's wish that unregistering is secure;
the product owner left the technical design to us.

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
| R9 | An attendee can unregister after registering, so that the seat goes to someone else. |
| R10 | To unregister, the request gives the cancellation token that the registration's 201 response returned. Without the right token, nothing is unregistered. Only the attendee who registered sees the token. |

## API

| Method | Path | Request body | Success status | Response body |
|--------|------|--------------|----------------|---------------|
| POST | `/workshops/{id}/registrations` | `{name, email}` | 201 | Registration with `cancellationToken` |
| GET | `/workshops/{id}/registrations` | | 200 | list of Registration, oldest first |
| DELETE | `/workshops/{id}/registrations/{registrationId}` | `{token}` | 204 | none |

`{id}` is the workshop's talk ID, and `{registrationId}` is the `id` of a Registration. A Registration
is `{id, workshopId, name, email}`. `email` is the
email as the attendee typed it. Only the 201 of POST adds `cancellationToken` to the Registration (R10).

## Our decisions

| Case | Decision | Why |
|------|----------|-----|
| Name missing, empty or only spaces | 400 | R1. Same as the speaker's name (Speakers R3). |
| Email missing, empty, only spaces, or not a real address | 400 | R1. The same rule as for speakers (`something@something.domain`, no spaces), from one place in the code, as the PO asked for speakers on 2026-11-06. |
| Missing body or malformed JSON | 400 | Same as the base features |
| ID in the path that is not a number | 400 | Same as the base features |
| Unknown talk in the path, on POST and GET | 404 | Same as the base features |
| POST with an unusable body to an unknown talk | 400 | The body is checked before the workshop is looked up (`WorkshopRegistrationController`), as in the base features. A request that is wrong in itself is a bad request, whatever it is for. |
| POST for a talk that exists but is not a `WORKSHOP` (R2) | 409, nothing is saved | The request names "only workshops" and "only accepted" in one rule, so a talk of another format refuses a registration like a closed workshop (R3). Changed from 404 on 2026-11-06: harness check `workshop-registration/check-2` was FAULTY at tick 137 on `POST /workshops/{id}/registrations` for an accepted `LIGHTNING_20`, which got our 404. |
| GET for a talk that exists but is not a `WORKSHOP` (R2) | 200 with `[]` | Nobody can register for it, so its list is empty, as for a workshop that was never accepted. Changed from 404 on 2026-11-06: harness check `workshop-registration/check-2` was FAULTY at tick 150 on `GET /workshops/{id}/registrations` for an accepted `LIGHTNING_10`, after our POST answered 409. An unknown talk ID still gives 404. |
| A workshop that is `SUBMITTED`, `WAITLISTED`, `REJECTED` or `WITHDRAWN` (R3) | 409 on POST, nothing is saved | A conflict with the current state, as in Acceptance R8 (rating a closed talk). |
| GET for a workshop that is not `ACCEPTED` | 200 with its list | R4 needs the list of a withdrawn workshop. A workshop that was never accepted has an empty list. |
| The 21st registration (R5) | 409, nothing is saved | A conflict with the current state, as for the limit of 3 talks (Talks R7). |
| The same email again, in any letter case (R6) | 409, nothing is saved | Same as the speakers' email (Speakers R5). |
| Email stored and shown | As typed. Compared in lower case. | The API says "as the attendee typed it". |
| The attendee is not a registered speaker | Allowed | Attendees are not speakers. The request asks for a name and an email only. |
| Order of the list (R8) | By registration ID, oldest first | IDs grow in the order the registrations are saved. |
| Who can read the list | Anyone, no authentication | Same as the other endpoints today (tech debt B11). |
| Concurrency (R5, R6, R9) | A registration and an unregistration both lock the talk row (`SELECT … FOR UPDATE`) and do their count, insert or delete in one transaction. A unique index on the workshop and the lower-case email backs R6. | Advisory locks are not available on the platform. A plain conditional `INSERT` lets two requests both see 19 seats taken, and a seat freed by R9 must go to one attendee only. |
| The workshop's own speakers | They take no seats: the 20 seats are for attendees only. | PO, Slack #team-11, 2026-11-06 14:00. |
| Unregister (R9) | `DELETE /workshops/{id}/registrations/{registrationId}` gives 204 with no body. The registration is gone from the list. | The PO asked for it (2026-11-06 14:01). The Registration already has an `id`, so the request's API extends naturally. |
| Unregister an unknown registration, one already unregistered, or one of another workshop | 404 | The resource does not exist under this path. Unregistering twice is not an error to hide: the second request finds nothing. |
| Unregister from an unknown talk or a talk that is not a `WORKSHOP` | 404 | An unknown talk does not exist, and a talk of another format has no registrations to delete. GET answers 200 `[]` for such a talk, but a DELETE names one registration, and there is none. |
| Unregister from a `WITHDRAWN` workshop | 204 | R4 keeps the others on the list; an attendee may still leave it. |
| The freed seat (R9) | Free at once: on a full workshop, the next registration after an unregistration gets 201. | The PO: "so that the seat goes to someone else". |
| Register again after unregistering (R6, R9) | 201 | The email is no longer on the list. |
| Who can unregister (R10) | Whoever gives the registration ID and the registration's cancellation token in a JSON body: `{"token": "..."}`. The token is compared exactly, letter case included. | Security review S5b: the public list (R8) shows every email, so an email is no proof. Only the attendee gets the token, in the 201 of their own registration. A body, not a query parameter, keeps the token out of URLs. |
| Wrong token, a body without `token`, or a `token` that is not a string (R10) | 404, nothing is unregistered | The same answer as for an unknown registration, so the answer does not tell whether a registration ID exists. |
| No body, or a body that is not JSON (R10) | 400 | Same as POST. The body is checked before the registration is looked up. |
| The cancellation token (R10) | 32 random bytes from `SecureRandom`, in URL-safe Base64 without padding: 43 characters. A new token for each registration. | 256 bits cannot be guessed. A random UUID has only 122 random bits, under the 128 bits that we require. |
| Where the token shows (R10) | Once, as `cancellationToken` in the 201 of POST. Never in `GET /workshops/{id}/registrations`, never in another response. | The list is public (R8). |
| How the token is stored (R10) | Only its SHA-256 hash, in the new column `workshop_registration.cancellation_token_hash`. The check compares hashes in constant time. | A copy of the database does not give anyone a token. |
| The token in our request log (R10) | Masked: the log shows `"cancellationToken":"***"` and `"token":"***"`. | `RequestLogFilter` logs the request and response bodies, and the container logs are not secret. |
| Registrations from before the token (R10) | They have no token hash, so no token unregisters them: 404. They stay on the list. | The new column is added with `ALTER TABLE … ADD COLUMN IF NOT EXISTS`, and old rows get `NULL`. Nobody received a token for them. Coordinator's decision, security review S5b, 2026-11-06. |

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
| `LIGHTNING_10`, `LIGHTNING_20` or `PRESENTATION` (accepted) | 409 | 200, `[]` |
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

### R9: unregister frees the seat

| Given | Request | Status |
|-------|---------|--------|
| a full workshop (20 attendees) | unregister one attendee, then register a new one | 204, then 201 |
| Ada registered | unregister Ada twice | 204, then 404 |
| Ada registered | unregister Ada, then Ada registers again | 204, then 201 |
| Ada registered for workshop 1 | `DELETE /workshops/2/registrations/<Ada's id>` | 404 |
| no registration with this ID | unregister it | 404 |
| Ada and Grace registered, then the workshop is withdrawn | unregister Ada | 204; the list shows only Grace |

### R10: unregister with the registration's cancellation token

Ada and Grace are registered for the same workshop. Each 201 gave a token. Every request unregisters Ada.

| Body of the DELETE | Status |
|--------------------|--------|
| `{"token": "<Ada's token>"}` | 204 |
| `{"token": "<Grace's token>"}` | 404 |
| `{"token": "<Ada's token in capitals>"}` | 404 |
| `{"email": "ada@example.com"}` | 404 |
| `{}` | 404 |
| `{"token": 42}` | 404 |
| no body | 400 |
| `{"token":` (not JSON) | 400 |

| Given | Body of the DELETE | Status |
|-------|--------------------|--------|
| a registration saved before the token existed | any token | 404 |

### R10: where the token shows

| Request | Has `cancellationToken`? |
|---------|--------------------------|
| POST, 201 | yes, 43 characters |
| GET the list | no |
| the request log line of the POST and of the DELETE | no, masked as `***` |

The R9 examples send the registration's own token.

## Effect on existing features

- Talks and Acceptance: no change. A registration reads the talk's format and status and changes
  neither. A withdrawal does not delete registrations (R4).
- POST `/workshops/{id}/registrations`: the 201 body has one more field, `cancellationToken` (R10). The
  fields of the request's Registration schema do not change.
- `/admin/stats` and the CSV export: no change. Registrations are not part of them. The PO's answer
  about withdrawn talks in the statistics (a separate line) is not about registrations.

## Open questions for the product owner

- [x] Do the workshop's own speakers take seats out of the 20? — No (2026-11-06 14:00). See "Our decisions".
- [x] Can an attendee cancel a registration and free the seat? — Yes, it must be possible (2026-11-06
  14:01). This is R9.
