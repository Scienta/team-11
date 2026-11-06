#!/usr/bin/env bash
# Starts a jar against the byte-equal database and waits for /health. Writes the pid and the log into the work
# directory. Stop it with stop.sh.
# Usage: workflow/byte-equal/run.sh <jar>
# Env:   PORT (default 8091), PGPORT (default 5433), WORK (default target/byte-equal),
#        JAVA (default: the java of JDK 25, or 27 when 25 is missing; the service needs Java 25 since Boot 4)
set -euo pipefail
jar="${1:?jar}"; port="${PORT:-8091}"; work="${WORK:-target/byte-equal}"
mkdir -p "$work"
PGHOST=localhost PGPORT="${PGPORT:-5433}" PGDATABASE=cfp PGUSER=cfp PGPASSWORD=cfp PGSSLMODE=disable \
  "${JAVA:-$( (/usr/libexec/java_home -v 25 || /usr/libexec/java_home -v 27) 2>/dev/null)/bin/java}" -jar "$jar" --server.port="$port" > "$work/app.log" 2>&1 &
echo $! > "$work/app.pid"
for _ in $(seq 60); do curl -fsS "localhost:$port/health" >/dev/null 2>&1 && exit 0; sleep 1; done
echo "the app did not start; see $work/app.log" >&2; exit 1
