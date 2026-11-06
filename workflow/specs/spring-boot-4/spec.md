# Upgrade to Spring Boot 4.x (`spring-boot-4`)

Source: `request.md` in this folder (the feature request from the harness, not edited).

## Rules

| ID | Rule |
|----|------|
| R1 | The service runs on Spring Boot 4 (the latest 4.x), on a Java version that is supported for it. |
| R2 | Nothing else changes for users: everything in the Speakers, Talk submissions and Ratings specs keeps working exactly as today, formats included. |
| R3 | `GET /actuator/info` reports the running versions in `versions.springBoot`, `versions.springFramework` and `versions.java`. They are read at run time, never hard-coded or faked. |

## API

No new or changed endpoints.

| Method | Path | Request body | Success status | Response body |
|--------|------|--------------|----------------|---------------|
| GET | `/actuator/info` | | 200 | `{"versions": {"java", "springBoot", "springFramework"}}` |

## Our decisions

The feature request does not give these. We decide them and write down why.

| Case | Decision | Why |
|------|----------|-----|
| Versions | Spring Boot 4.1.2 (latest 4.x release), Spring Framework 7.0.10 and Jackson 3.1.7 from the Boot BOM, Java 25 | R1. The user chose Java 25 (an LTS supported by Boot 4). 4.2.0 is still a release candidate. |
| Where the versions come from | `VersionInfoContributor`, unchanged: `SpringBootVersion.getVersion()`, `SpringVersion.getVersion()` and the `java.version` system property | R3. They read the jar manifests and the JVM at run time. |
| Trailing slash on any path (`/speakers/`, `/speakers/{id}/`, `/talks/{id}/` …) | Same answer as without the slash, for every method, as on Boot 2.7 | Speakers R7. Spring 6+ no longer matches a trailing slash, so a `UrlHandlerFilter` strips it before the request reaches the controllers. It wraps the request, it does not redirect, so a `POST` or `PUT` keeps its body and status. |
| `path` in an error body for a request with a trailing slash | Keeps the slash, as on Boot 2.7 | R2: the error body shows the path the client sent. |
| Jackson 3 defaults | `spring.jackson.use-jackson2-defaults=true` | R2. Boot's switch for the defaults Boot 2.7 used with Jackson 2. Without it, Jackson 3 writes dates as `…Z`. |
| `submittedAt` | `yyyy-MM-dd'T'HH:mm:ss.SSS+00:00`, never `Z` | Talks spec. Kept by the Jackson 2 defaults. |
| `submittedOn` | `yyyy-MM-dd` | Talks spec. Jackson 3 writes a `java.sql.Date` as a full timestamp (`2026-11-05T23:00:00.000+00:00`) even with the Jackson 2 defaults, so `SqlDateJsonModule` writes its `toString()`, as Jackson 2 did. |
| Property order in JSON bodies | As on Boot 2.7: the field order of `TalkDto`, the insertion order of the maps | R2. Checked as text by `UpgradeContractTest`. |
| Path variables without a name (`@PathVariable Long id`) | Compile with `-parameters` | Spring 6.1+ reads the names only from `-parameters`; without it every such request is a 500. The starter parent sets it; we import the BOM instead, so the pom sets it. |
| `averageRating: null` | Stays in the body | Ratings R4 |
| Score `3.0`, `"3"`, `true`, `null`, `[3]` | 400 `score must be a whole number` | Ratings R2. The body stays a `Map`, so Jackson does not coerce. |
| Unknown fields in a request body | Ignored | As on Boot 2.7 |
| Comments in a request body | Allowed | As on Boot 2.7 (`allow-comments`, kept for the mobile client) |
| A number where a text field is expected (`"title": 5`), a text number where a number is expected (`"speakerId": "7"`) | Coerced, as on Boot 2.7 | R2 |
| Error bodies | The Boot error body, not RFC 9457 Problem Details: `timestamp`, `status`, `error`, `message`, `path`, plus `errors` for a `@Valid` failure. Our own messages unchanged. | R2. `include-message` and `include-binding-errors` stay `always`. |
| Error messages written by Spring or Jackson (malformed JSON, a path variable that is not a number, an unknown path, a wrong method) | Same status; the wording changes | The text names library classes (`com.fasterxml.jackson…`, "nested exception is …") that do not exist in Spring 7 and Jackson 3. No spec defines it. Examples: an unknown path says `No static resource nope.` instead of `No message available`; `DELETE /speakers` says `Method 'DELETE' is not supported.` instead of `Request method 'DELETE' not supported`. |
| `Content-Type` of a success | `application/json;charset=UTF-8`, as on Boot 2.7 | R2. The forced UTF-8 encoding moves to the Boot 4 property name. |
| `Content-Type` of an error body (400, 403, 404, 405, 409, 500) | `application/json;charset=UTF-8`, as on Boot 2.7 | R2. Carl Kristian decided (2026-11-06). Spring 7 resets the response before the error page, which drops the forced charset, so `ErrorCharsetConfig` sets UTF-8 again on the error dispatch only. |
| `ratedAtDisplay` | `M/d/yy, h:mm AM` with an ordinary space before AM/PM, on Java 25 | Ratings R8. The explicit pattern in `Utils.usShort` does not use the locale's SHORT style. |
| `GET /health` | Unchanged: 200 `{"status":"ok","version":"<GIT_SHA>"}`, 503 `db-unavailable` within 5 s | Deploy contract. `HealthTest` covers it. |

## Examples

### R1: the running versions

| Field on `/actuator/info` | Major version |
|---------------------------|---------------|
| `versions.springBoot` | 4 |
| `versions.springFramework` | 7 |
| `versions.java` | 25 |

### R2: a trailing slash answers like the path without it

| Method | Path | Status |
|--------|------|--------|
| POST | `/speakers/` | 201 |
| GET | `/speakers/{id}/` | 200, the same body as `/speakers/{id}` |
| PUT | `/speakers/{id}/` | 200 |
| GET | `/speakers/{id}/talks/` | 200 |
| POST | `/talks/` | 201 |
| GET | `/talks/{id}/` | 200 |
| GET | `/talks/` | 200 |
| PUT | `/talks/{id}/ratings/{reviewerId}/` | 200 |
| GET | `/talks/?includeWithdrawn=false` | 200 |
| PUT | `/talks/{id}/status/` | 200 |
| POST | `/talks/{id}/withdraw/` | 200 |
| PUT | `/talks/{id}/co-speakers/{speakerId}/` | 200 |
| DELETE | `/talks/{id}/co-speakers/{speakerId}/` | 200 |
| GET | `/speakers/999999/` | 404, `"path":"/speakers/999999/"` |

### R2: formats and property order

| Response | Body |
|----------|------|
| `POST /talks` | `{"id":…,"title":…,"format":…,"level":…,"speakerId":…,"durationMinutes":…,"status":"SUBMITTED","submittedAt":"2026-11-06T12:31:32.907+00:00","submittedOn":"2026-11-06","averageRating":null,"ratingCount":0,"coSpeakerIds":[]}` (`coSpeakerIds` last, from co-speakers) |
| `POST /speakers` | `{"id":…,"name":…,"email":…,"bio":null}` |
| `PUT /talks/{id}/ratings/{reviewerId}` | `{"talkId":…,"reviewerId":…,"score":3,"ratedAtDisplay":"11/6/26, 12:31 PM"}` |
| `GET /speakers/999999` | `{"timestamp":"2026-11-06T12:31:32.825+00:00","status":404,"error":"Not Found","message":"speaker not found","path":"/speakers/999999"}` |

### R2: binding

| Request | Status | Message |
|---------|--------|---------|
| score `3` | 200 | |
| score `3.0` | 400 | `score must be a whole number` |
| score `"3"` | 400 | `score must be a whole number` |
| score `true` | 400 | `score must be a whole number` |
| score `null` | 400 | `score must be a whole number` |
| score `[3]` | 400 | `score must be a whole number` |
| score `6` | 400 | `score must be 1-5` |
| speaker with an unknown field | 201 | |
| speaker with a `/* comment */` | 201 | |
| speaker with an empty name | 400 | `Validation failed for object='speakerForm'. Error count: 1`, with `errors` |
| talk with `"speakerId": "<id>"` | 201 | |
| talk with `"title": 5` | 201 | |

### R2: every error is JSON in UTF-8

Each row answers `Content-Type: application/json;charset=UTF-8` and the Boot error body.

| Request | Status |
|---------|--------|
| `POST /speakers` with email `not-an-email` | 400 |
| `POST /speakers` with an empty name (`@Valid`) | 400 |
| `POST /speakers` with malformed JSON | 400 |
| `GET /speakers/abc` | 400 |
| `POST /talks/{id}/withdraw` by another speaker | 403 |
| `GET /speakers/999999` | 404 |
| `GET /nope` | 404 |
| `DELETE /speakers` | 405 |
| `POST /speakers` with a taken email | 409 |
| `GET /speakers/search?q='` (tech-debt B1) | 500 |

### R3: the versions are the real ones

| Field on `/actuator/info` | Equals |
|---------------------------|--------|
| `versions.springBoot` | `SpringBootVersion.getVersion()` |
| `versions.springFramework` | `SpringVersion.getVersion()` |
| `versions.java` | `System.getProperty("java.version")` |

## Effect on existing features

All three base features. The old behaviour is guarded by `SpeakersTest`, `TalksTest`, `RatingsTest`,
`HealthTest` and `UpgradeContractTest` (R2 above), and by `examples/*.hurl` against a container built
from the `Dockerfile`.

## Open questions for the product owner

- [x] For the team, not the product owner: is `application/json` without `;charset=UTF-8` on error bodies acceptable? No. Carl Kristian (2026-11-06): error bodies keep `;charset=UTF-8`, as on Boot 2.7.
