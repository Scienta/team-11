# Speakers (`speakers`)

Source: `request.md` in this folder (the base spec from the harness, not edited).

## Rules

| ID | Rule |
|----|------|
| R1 | A speaker registers with a name and an email, and may add a bio. |
| R2 | A speaker can look up the profile later, exactly as registered. |
| R3 | Name and email are required. The email must be a real email address. The bio is optional. |
| R4 | A bio is at most 500 characters. |
| R5 | One email is one speaker, whatever the letter case. |
| R6 | Speakers stay registered after a deploy or a restart. |
| R7 | `/speakers/` and `/speakers/{id}/` work like the paths without the slash. |

## API

| Method | Path | Request body | Success status | Response body |
|--------|------|--------------|----------------|---------------|
| POST | `/speakers` | `{name, email, bio?}` | 201 | `{id, name, email, bio}` |
| GET | `/speakers/{id}` | | 200 | `{id, name, email, bio}` |

## Our decisions

| Case | Decision | Why |
|------|----------|-----|
| Name missing, empty or only spaces | 400 | R3 |
| Email missing, empty or only spaces | 400 | R3 |
| Email that is not an address | 400 | R3. An address is: one `@`, a local part, and a domain with at least two labels split by dots. No spaces. |
| Email `ada@localhost` | 400 | The domain has one label. A conference speaker has a public address. |
| Email with a local part over 64 characters, or a domain over 255 | 400 | R3. RFC 5321 sets these limits, so a longer address is not a real address. The log masks only addresses inside the limits (security review, 2026-11-06). The harness sends local parts of 14 characters. |
| Bio of 501 characters | 400 | R4 |
| What a "character" is | One Unicode code point | An emoji is one character, not two UTF-16 units. |
| The same email, other letter case | 409 | R5. A conflict with an existing speaker, not a bad request. |
| Unknown speaker ID | 404 | |
| Speaker ID that is not a number | 400 | |
| `PUT /speakers/{id}` (admin, not in the spec) | Removed: 405, the speaker does not change | It had no authorisation, so anybody could change any speaker's name and email (security review, 2026-11-06). No spec asks for it. |
| Name, email and bio | Stored and returned as sent, not trimmed | R2: "exactly as registered" |
| `GET /speakers/search?q=` (admin, not in the spec) | `q` is bound as a parameter, so `O'Brien` finds O'Brien. `%` and `_` in `q` still act as wildcards. | B1: the term was pasted into the SQL (injection). Escaping wildcards would be a second behaviour change; nobody has asked for it. |

## Examples

### R3: the email must be a real address

| Email | Status |
|-------|--------|
| `ada@example.com` | 201 |
| `ada.lovelace+cfp@mail.example.co.uk` | 201 |
| `not-an-email` | 400 |
| `ada@example` | 400 |
| `@example.com` | 400 |
| `ada@.com` | 400 |
| `ada@example.` | 400 |
| `ada@example..com` | 400 |
| `ada@@example.com` | 400 |
| `ada lovelace@example.com` | 400 |
| `a` × 64 + `@example.com` | 201 |
| `a` × 65 + `@example.com` | 400 |
| `ada@` + `e` × 251 + `.com` (domain of 255) | 201 |
| `ada@` + `e` × 252 + `.com` (domain of 256) | 400 |

### R4: a bio is at most 500 characters

| Bio | Status |
|-----|--------|
| none | 201 |
| 500 × `x` | 201 |
| 501 × `x` | 400 |
| 500 × 😀 (1000 UTF-16 units) | 201 |
| 501 × 😀 | 400 |

### R5: one email is one speaker

| Given | Request | Status |
|-------|---------|--------|
| `ada@example.com` is registered | POST with `ADA@Example.com` | 409 |
| nothing | 10 POSTs with one email at the same time | one 201, nine 409 |

## Effect on existing features

None. The talks and ratings features read speakers only by ID.

## Open questions for the product owner

- [ ] Is a character a code point or a grapheme (a family emoji is 7 code points)? We count code points.
- [x] Must we accept an email with a one-label domain, such as `ada@localhost`? We reject it. Product owner (Stein, #team-11, 2026-11-06 12:58): for now "noe@noe.domene" (something@something.domain) is enough, and the rule should sit in one place so it's easy to change. That's `Utils.EMAIL`.
