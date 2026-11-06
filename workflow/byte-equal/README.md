# Byte-equal check for a refactoring

Use these scripts when a refactoring must not change any answer of the talk lists and the reports. They run the
jar from before and the jar from after against the same seeded database, capture 16 answers from each, and compare
them byte for byte.

## What the scripts cover

`seed.sh` makes 3 speakers and 7 talks: one talk in each status (`SUBMITTED`, `ACCEPTED`, `REJECTED`,
`WAITLISTED`, `WITHDRAWN`), several ratings, and one co-speaker.

`capture.sh` records the status and the body of these requests:

- `GET /talks` with `sort=rating`, `speakerId`, `includeWithdrawn`, and the combinations of them
- the 400 answers for `sort=bogus` and `includeWithdrawn=yes`
- `GET /talks/{id}`, `GET /speakers/{id}/talks`, and the 404 for an unknown speaker
- `GET /admin/stats` and `GET /admin/talks.csv`

The script blanks the fields `"timestamp"` (error answers) and `"generated"` (stats), because they change with
each run. Every other byte must be the same.

## Steps

Run them from the repo root. Docker and Java 17 must be available, so run them outside the sandbox.

1. Start an empty database on port 5433, apart from the usual local one:

   ```bash
   docker run -d --rm --name cfp-byte-equal -p 5433:5432 \
     -e POSTGRES_DB=cfp -e POSTGRES_USER=cfp -e POSTGRES_PASSWORD=cfp postgres:16-alpine
   ```

2. Build the jar from before the change, and keep a copy:

   ```bash
   JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw -q package -DskipTests
   mkdir -p target/byte-equal && cp target/cfp.jar target/byte-equal/before.jar
   ```

3. Make the change, then build the jar from after it:

   ```bash
   JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw -q package -DskipTests
   cp target/cfp.jar target/byte-equal/after.jar
   ```

4. Compare. The script seeds the database with the "before" jar, captures both jars, and runs `diff -r`:

   ```bash
   workflow/byte-equal/check.sh target/byte-equal/before.jar target/byte-equal/after.jar
   ```

   It prints `byte-equal: 16 answers` and exits 0 when nothing differs. Otherwise it prints the diff.

5. Stop the database: `docker stop cfp-byte-equal`.

For a second comparison, start a new empty database first, because `check.sh` seeds it.

## Notes

- `./mvnw clean` deletes `target/byte-equal`, the saved jars included.
- To compare one step at a time, run `run.sh`, `capture.sh <url> <dir>` and `stop.sh` yourself for each jar, and
  `diff -r` the directories.
- `PORT` (default 8091), `PGPORT` (default 5433) and `WORK` (default `target/byte-equal`) change the defaults.
- A change that adds a field to the talk JSON is not a refactoring: the diff shows it.
