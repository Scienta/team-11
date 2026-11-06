# <Feature name> (`<feature-slug>`)

Source: `request.md` in this folder (the feature request from the harness, not edited).

## Rules

Copy each business rule from the feature request. Give it an ID so that tests, Hurl requests and
commits can refer to it.

| ID | Rule |
|----|------|
| R1 |      |

## API

| Method | Path | Request body | Success status | Response body |
|--------|------|--------------|----------------|---------------|
|        |      |              |                |               |

## Our decisions

The feature request does not give these. We decide them and write down why.

| Case | Decision | Why |
|------|----------|-----|
| Unknown ID in the path | 404 | Same as the base features |
| Missing or malformed field | 400 | Same as the base features |
| Rule violation (conflict with current state) | 409 | |

## Examples

One table for each rule. Each row becomes a Hurl request in `examples/our-<feature-slug>.hurl` and a row
in a JUnit test.

### R1

| Given | Request | Status | Response |
|-------|---------|--------|----------|
|       |         |        |          |

## Effect on existing features

List each base feature or earlier drip feature that this feature changes, and the test that shows
the old behaviour still works.

## Open questions for the product owner

- [ ] Question — answer (time of answer)
