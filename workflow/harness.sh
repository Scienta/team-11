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

# No temp files: curl appends a last line with the status and the Retry-After header (curl 7.84+).
for attempt in 1 2 3 4 5; do
  response="$(curl -sS -w '\n%{http_code} %header{retry-after}' \
    -H "Authorization: Bearer $(cat "$token_file")" "$harness$path")"
  meta="${response##*$'\n'}"
  body="${response%$'\n'*}"
  code="${meta%% *}"
  retry_after="${meta#* }"
  case "$code" in
    2??) printf '%s\n' "$body"; exit 0 ;;
    429)
      echo "429, waiting ${retry_after:-2} s (attempt $attempt)" >&2
      sleep "${retry_after:-2}"
      ;;
    *) echo "HTTP $code for $path" >&2; printf '%s\n' "$body" >&2; exit 1 ;;
  esac
done
exit 1
