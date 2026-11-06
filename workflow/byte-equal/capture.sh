#!/usr/bin/env bash
# Writes the status and body of each list and report request into one file per request, with the time fields
# ("timestamp", "generated") blanked, so that two runs can be compared byte for byte.
# Usage: workflow/byte-equal/capture.sh <base-url> <out-dir>
set -euo pipefail
b="${1:?base url}"; out="${2:?output directory}"
mkdir -p "$out"; : > "$out/00-paths.txt"; i=0
for p in "/talks" "/talks?sort=rating" "/talks?includeWithdrawn=false" "/talks?includeWithdrawn=true" \
         "/talks?sort=rating&includeWithdrawn=false" "/talks?speakerId=1" "/talks?speakerId=1&sort=rating" \
         "/talks?speakerId=1&includeWithdrawn=false" "/talks?sort=bogus" "/talks?includeWithdrawn=yes" \
         "/talks/1" "/talks/3" "/speakers/1/talks" "/speakers/99/talks" "/admin/stats" "/admin/talks.csv"; do
  i=$((i+1))
  curl -sS -w '\nHTTP %{http_code}\n' "$b$p" \
    | sed -E 's/"timestamp":"[^"]*"/"timestamp":"-"/; s/"generated":"[^"]*"/"generated":"-"/' > "$out/$(printf %02d $i).txt"
  echo "$p" >> "$out/00-paths.txt"
done
