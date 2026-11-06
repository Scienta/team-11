# Warm-up at start-up (`warm-up`)

Source: this is not a harness feature, so there is no `request.md`. The source is the FAULTY tick 27
in `REVIEW.md` section 4. After the deploy of `726c5bc`, about 25 concurrent `POST /speakers` arrived
30 s after the start. Each took 3.0–4.2 s, against 0.1–0.3 s on a warm JVM. All of them got 201, but
`speakers/check-2` and `ratings/check-1` were FAULTY for one tick.

## Rules

| ID | Rule |
|----|------|
| R1 | When the service has started, it sends one warm-up request to each endpoint that the harness checks. The requests go through its own HTTP port, so they use the same filters, JSON, validation and database pool as the harness requests. |
| R2 | The warm-up changes no data. Each warm-up request is rejected or reads only. |
| R3 | When the warm-up ends, the service writes one log line: `Warm-up done: <n> requests in <t> ms`. |
| R4 | The warm-up never stops the service. A warm-up request that fails, or gets no answer in 5 s, gives one `WARN` line, and the warm-up goes on. |
| R5 | On the deployed service, the first burst of harness requests after a start answers each request in less than 1 s. |

## API

No new endpoint. `GET /health` does not change: it answers 200 while the database answers, also
during the warm-up.

## Our decisions

| Case | Decision | Why |
|------|----------|-----|
| `/health` during the warm-up | 200 when the database answers, as before | The deploy contract allows 503 only with `db-unavailable`. Start-up already takes 38 s of the 60 s that the deploy gate waits. |
| When the warm-up runs | In a background thread, after the web server has started | The service does not start later. The harness traffic came 30 s after the start, so a warm-up of a few seconds ends first. |
| A request arrives during the warm-up | The service answers it as usual | The warm-up only prepares the code paths. It does not hold back traffic. |
| Which requests | One for each row in the R1 example | Each one is rejected or reads only (R2). Each one reaches a different controller method. |
| How many rounds | One | We measure with R5 first. We add rounds only when the first burst is still slow. |
| Warm-up lines in the request log | They show as usual, with a 4xx status for the rejected requests | The request log is how we check R1 on the deployed service. |
| Status codes of the warm-up requests | The service's own codes, as in the base features | A different code is a warning (R4), not a stop. |

## Examples

### R1: the warm-up requests

| Request | Request body | Status |
|---------|--------------|--------|
| `POST /speakers` | `{}` | 400 |
| `GET /speakers/0` | | 404 |
| `POST /talks` | `{}` | 400 |
| `GET /talks/0` | | 404 |
| `GET /talks?speakerId=0&sort=rating` | | 200 |
| `PUT /talks/0/ratings/1` | `{"score":3}` | 404 |

Each request shows in the request log (`RequestLogFilter`) after the start, before any test or
harness request.

### R2: no data changes

| Table | Rows after the warm-up |
|-------|------------------------|
| `speaker`, `talk`, `rating`, `vote` | 0 |

### R3: the log line at the end

| Given | Log line |
|-------|----------|
| All 6 requests answered | `Warm-up done: 6 requests in <t> ms` |

### R4: a failed warm-up request

| Given | Result |
|-------|--------|
| A warm-up request gets no answer in 5 s | One `WARN` line with the method and the path. The service runs, and `/health` answers 200. |

We write the R4 test with the implementation, as a test of the warm-up class with a request that
fails. It needs a seam that does not exist yet.

### R5: the first burst on the deployed service

| Given | Check | Result |
|-------|-------|--------|
| A deploy, then the first harness tick | `.github/fagdag/logs.sh --since <start time>` | `Warm-up done` comes before the first harness request. Each `POST /speakers` in the first burst takes less than 1000 ms. |

R5 has no JUnit test. In a test run the JVM is already warm from the earlier test classes, so a time
limit there tests nothing. We check R5 in the logs after each deploy.

## Effect on existing features

None in the API. The request log gets 6 more lines after each start. The 5xx watch on the logs is
not affected, because the warm-up gets no 5xx.

## Open questions for the product owner

- [ ] What is the harness time limit for one request? We use 1 s in R5 until we know.
