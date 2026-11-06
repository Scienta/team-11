#!/usr/bin/env bash
# Reads the fagdag harness API without printing the team token.
#
# Usage:
#   workflow/harness.sh spec <feature>              the spec or feature request (markdown)
#   workflow/harness.sh examples <feature>          the published Hurl examples, if any
#   workflow/harness.sh feedback                    the check results for our team (JSON)
#   workflow/harness.sh timeline <feature>/<check>  the history and transcript of one check (JSON)
#
# The API allows bursts of 5 requests, then 1 per second. On 429 the script waits for Retry-After
# and tries again, at most 5 times.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
token_file="$root/.fagdag/harness-token"
if [[ ! -s "$token_file" ]]; then
  echo "No token: paste it from the harness /feedback page into .fagdag/harness-token" >&2
  exit 1
fi
harness="$(cat "$root/.fagdag/harness-url")"
team="$(cat "$root/.fagdag/team")"

case "${1:-}" in
  spec)     path="/specs/${2:?feature}/spec.md" ;;
  examples) path="/specs/${2:?feature}/examples.hurl" ;;
  feedback) path="/api/teams/$team/feedback" ;;
  timeline) path="/api/teams/$team/feedback/timeline?check=${2:?feature/check}" ;;
  *) sed -n '2,10p' "$0" >&2; exit 2 ;;
esac

headers="$(mktemp)" body="$(mktemp)"
trap 'rm -f "$headers" "$body"' EXIT
for attempt in 1 2 3 4 5; do
  code="$(curl -sS -o "$body" -D "$headers" -w '%{http_code}' \
    -H "Authorization: Bearer $(cat "$token_file")" "$harness$path")"
  case "$code" in
    2??) cat "$body"; exit 0 ;;
    429)
      wait="$(awk 'tolower($1)=="retry-after:" {print $2+0}' "$headers")"
      echo "429, waiting ${wait:-2} s (attempt $attempt)" >&2
      sleep "${wait:-2}"
      ;;
    *) echo "HTTP $code for $path" >&2; cat "$body" >&2; exit 1 ;;
  esac
done
exit 1
