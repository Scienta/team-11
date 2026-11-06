#!/usr/bin/env bash
# Stops the jar that run.sh started.
# Env: WORK (default target/byte-equal)
set -euo pipefail
work="${WORK:-target/byte-equal}"
kill "$(cat "$work/app.pid")"
rm -f "$work/app.pid"
