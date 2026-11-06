#!/usr/bin/env bash
# Seeds an empty database with the "before" jar, captures the answers of both jars, and compares them byte for byte.
# Exits 0 only when every answer is the same.
# Usage: workflow/byte-equal/check.sh <before.jar> <after.jar>
# Env:   PORT (default 8091), WORK (default target/byte-equal)
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
before="${1:?before jar}"; after="${2:?after jar}"
work="${WORK:-target/byte-equal}"; url="http://localhost:${PORT:-8091}"
rm -rf "$work/before" "$work/after"
"$here/run.sh" "$before"; "$here/seed.sh" "$url"; "$here/capture.sh" "$url" "$work/before"; "$here/stop.sh"
sleep 2
"$here/run.sh" "$after"; "$here/capture.sh" "$url" "$work/after"; "$here/stop.sh"
diff -r "$work/before" "$work/after"
echo "byte-equal: $(($(ls "$work/before" | wc -l) - 1)) answers"
