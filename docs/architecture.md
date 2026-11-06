# Architecture

The CFP backend is one Spring Boot 2.7 service (Java 17) with one Postgres database. It runs as one
Scaleway serverless container. The code is in one package, `no.scienta.cfp`, in three layers:
controllers, one service and DAOs on `JdbcTemplate`. There is no JPA.

## Runtime

```mermaid
flowchart LR
    subgraph clients["Callers"]
        harness["Harness checks"]
        rollout["Deploy rollout<br/>(polls /health)"]
        committee["Committee tools<br/>(admin endpoints)"]
    end

    subgraph container["Scaleway serverless container: 1 instance, max 80 concurrent requests, port 8080"]
        filter["RequestLogFilter<br/>one log line per request"]

        subgraph web["Controllers"]
            speakerCtl["SpeakerController<br/>POST /speakers<br/>GET, PUT /speakers/{id}<br/>GET /speakers/{id}/talks<br/>GET /speakers/search"]
            talkCtl["TalkController<br/>POST, GET /talks<br/>GET /talks/{id}<br/>PUT /talks/{id}/ratings/{reviewerId}"]
            reportCtl["ReportController<br/>GET /admin/stats<br/>GET /admin/talks.csv"]
            healthCtl["HealthController<br/>GET /health<br/>SELECT 1, 5 s timeout,<br/>one probe at a time"]
        end

        service["ConferenceService<br/>validation, business rules,<br/>durations, averages, ranking"]
        notify["NotificationService<br/>logs only, SMTP is gone"]

        subgraph dao["DAOs (JdbcTemplate)"]
            speakerDao["SpeakerDao"]
            talkDao["TalkDao<br/>talk limit: row lock<br/>on the speaker"]
            ratingDao["RatingDao<br/>upsert per talk<br/>and reviewer"]
        end

        pool["Hikari pool<br/>max 5 connections"]
    end

    subgraph db["Postgres (PG* env vars, TLS, keeps data across deploys)"]
        tables[("speaker<br/>talk<br/>rating<br/>vote (legacy, unused)")]
    end

    harness --> filter
    rollout --> filter
    committee --> filter
    filter --> speakerCtl & talkCtl & reportCtl & healthCtl
    speakerCtl & talkCtl & reportCtl --> service
    service --> notify
    service --> speakerDao & talkDao & ratingDao
    speakerDao & talkDao & ratingDao --> pool
    healthCtl --> pool
    pool --> tables
```

## Deploy and logs

```mermaid
flowchart LR
    dev["Push to main"] --> actions["GitHub Actions: deploy<br/>docker build --build-arg GIT_SHA"]
    actions --> registry["Scaleway registry<br/>app:SHA and app:latest"]
    registry --> rolloutSh["rollout.sh<br/>redeploy, wait for /health<br/>to show the new SHA"]
    rolloutSh -- "fails" --> rollback["app:latest back to<br/>the previous version"]
    rolloutSh --> svc["Container on<br/>fagsem-h2026-team-11.scienta.cloud"]
    actions --> tests["Job: tests<br/>after the deploy, not a gate"]
    svc -- "stdout" --> cockpit["Scaleway Cockpit"]
    cockpit --> logs[".github/fagdag/logs.sh"]
```

## Facts that matter when you change the code

- `schema.sql` runs on every start-up (`spring.sql.init.mode=always`). Each statement must be
  idempotent (`IF NOT EXISTS`). Add new statements at the bottom.
- The talk limit holds under concurrent requests because `TalkDao.insertIfBelow` locks the speaker
  row (`SELECT … FOR UPDATE`) in the service transaction. Advisory locks are not available.
- A rating is one `INSERT … ON CONFLICT DO UPDATE`, so a double-click cannot count twice.
- `averageRating` and `ratingCount` come from one query per talk. `GET /talks` makes 1 + N queries.
- `ratedAtDisplay` uses the explicit pattern `M/d/yy, h:mm a` with `Locale.US`. Do not use the
  locale's SHORT style: from Java 20 it puts a narrow no-break space before AM/PM.
- `/admin/*` and `/speakers/search` are not in the specs. `/speakers/search` builds its SQL with
  string concatenation.
