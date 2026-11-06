#!/usr/bin/env bash
# Writes docs/status.md: the test status for the product owner, in plain Norwegian, one section per
# feature. It reads the TableTest report in docs/specs, the rule texts in
# workflow/po-status-rules.tsv and the harness feedback. Run it after the report; see
# workflow/process.md step 6.
#
# Usage: workflow/po-status.sh
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
specs="$root/docs/specs"
rules="$root/workflow/po-status-rules.tsv"
out="$root/docs/status.md"

[[ -f "$specs/index.md" ]] || { echo "No report in $specs: run the reporter first" >&2; exit 1; }

sections="Foredragsholdere Foredrag Karakterer Godkjenning Oppstart Annet"

section_of() {
  case "$1" in
    speakers) echo Foredragsholdere ;;
    talks) echo Foredrag ;;
    ratings | ratings-*) echo Karakterer ;;
    acceptance) echo Godkjenning ;;
    warm-up | warm-up-*) echo Oppstart ;;
    *) echo Annet ;;
  esac
}

feature_name() {
  case "$1" in
    speakers) echo Foredragsholdere ;;
    talks) echo Foredrag ;;
    ratings) echo Karakterer ;;
    acceptance) echo Godkjenning ;;
    co-speakers) echo Medforedragsholdere ;;
    workshop-registration) echo "Påmelding til workshop" ;;
    spring-boot-4) echo "Teknisk oppgradering" ;;
    *) echo "$1" ;;
  esac
}

rule_text() {
  awk -F'\t' -v page="$1" '$1 == page { print $2; exit }' "$rules"
}

# The pages in the order of the rules file, then the pages that the file does not name.
ordered_pages() {
  {
    grep -v '^#' "$rules" | cut -f1
    (cd "$specs" && find . -mindepth 2 -name '*.md' ! -name index.md | sed 's|^\./||; s|\.md$||' | sort)
  } | awk '!seen[$0]++' | while read -r page; do
    if [[ -f "$specs/$page.md" ]]; then echo "$page"; fi
  done
}

passing_rows() { grep -c '^| ✅ passes |' "$1" || true; }
failing_rows() { grep -c '^| ❌ fails |' "$1" || true; }

examples() {
  if (( $1 == 1 )); then echo "1 eksempel"; else echo "$1 eksempler"; fi
}

rule_line() {
  local page="$1" file="$specs/$1.md" title id text pass fail icon count
  title="$(sed -n 's/^## //p' "$file" | head -1)"
  id="$(printf '%s' "$title" | sed -n 's/^\(R[0-9][0-9]*\):.*/\1/p')"
  text="$(rule_text "$page")"
  if [[ -z "$text" ]]; then
    echo "No Norwegian text for $page in $rules" >&2
    text="${title#*: }"
  fi
  pass="$(passing_rows "$file")"
  fail="$(failing_rows "$file")"
  if (( pass + fail == 0 )); then
    icon="⚪"; count="ikke kjørt"
  elif (( fail == 0 )); then
    icon="✅"; count="$pass av $(examples "$pass")"
  else
    icon="❌"; count="$pass av $(examples $((pass + fail)))"
  fi
  printf -- '- %s %s%s — %s · [se eksemplene](specs/%s.md)\n' \
    "$icon" "${id:+**$id** }" "$text" "$count" "$page"
}

made_at() {
  local months=(januar februar mars april mai juni juli august september oktober november desember)
  local day month year time
  read -r day month year time <<<"$(TZ=Europe/Oslo date '+%-d %-m %Y %H:%M')"
  echo "$day. ${months[$((month - 1))]} $year kl. $time"
}

made_from() {
  local sha short repo note=""
  sha="$(git -C "$root" rev-parse HEAD)"
  short="$(git -C "$root" rev-parse --short HEAD)"
  repo="$(git -C "$root" remote get-url origin | sed -E 's#^git@github.com:#https://github.com/#; s#\.git$##')"
  if [[ -n "$(git -C "$root" status --porcelain -- src pom.xml)" ]]; then
    note=", med endringer som ikke er lagret i en versjon ennå"
  fi
  echo "versjon [$short]($repo/commit/$sha)$note"
}

harness_line() {
  local feedback
  # The token is gitignored, so it is only in the main checkout, also when this runs in a worktree.
  local main_root
  main_root="$(cd "$(git -C "$root" rev-parse --path-format=absolute --git-common-dir)/.." && pwd)"
  if ! feedback="$("$main_root/workflow/harness.sh" feedback 2>/dev/null)"; then
    echo "⚪ Arrangørenes kontroller: ikke lest, fordi vi ikke fikk kontakt."
    return
  fi
  local down total ok checked failed
  down="$(jq -r '.down.current' <<<"$feedback")"
  total="$(jq '[.features[].checks // [] | .[]] | length' <<<"$feedback")"
  ok="$(jq '[.features[].checks // [] | .[] | select(.status == "OK")] | length' <<<"$feedback")"
  checked="$(jq -r '.features[] | select(.checks != null) | .slug' <<<"$feedback" \
    | while read -r slug; do feature_name "$slug"; done | paste -sd, - | sed 's/,/, /g')"
  failed="$(jq -r '.features[] | select([.checks // [] | .[] | select(.status != "OK")] | length > 0) | .slug' <<<"$feedback" \
    | while read -r slug; do feature_name "$slug"; done | paste -sd, - | sed 's/,/, /g')"
  if [[ "$down" == "true" ]]; then
    echo "❌ Arrangørenes kontroller når ikke tjenesten vår akkurat nå."
  elif (( total == 0 )); then
    echo "⚪ Arrangørenes kontroller: ingen kontroller ennå."
  elif (( ok == total )); then
    echo "✅ Arrangørenes kontroller: alle $total er i orden ($checked)."
  else
    echo "❌ Arrangørenes kontroller: $((total - ok)) av $total er ikke i orden ($failed)."
  fi
}

write_page() {
  local pages page section lines all_pass=0 all_rows=0 rules_red=0 rules_count=0
  pages="$(ordered_pages)"
  while read -r page; do
    all_pass=$((all_pass + $(passing_rows "$specs/$page.md")))
    all_rows=$((all_rows + $(passing_rows "$specs/$page.md") + $(failing_rows "$specs/$page.md")))
    if (( $(failing_rows "$specs/$page.md") > 0 )); then rules_red=$((rules_red + 1)); fi
    rules_count=$((rules_count + 1))
  done <<<"$pages"

  echo "# Teststatus"
  echo
  echo "Laget $(made_at) fra $(made_from)."
  echo
  if (( rules_red == 0 )); then
    echo "✅ Våre tester: alle $rules_count regler er i orden ($all_pass av $all_rows eksempler)."
  else
    echo "❌ Våre tester: $rules_red av $rules_count regler har eksempler som feiler ($all_pass av $all_rows eksempler er i orden)."
  fi
  echo
  harness_line
  echo
  echo "Hver linje er en regel vi tester. Et eksempel er ett konkret tilfelle av regelen."
  echo "Arrangørenes kontroller sjekker tjenesten som kjører nå, og bare funksjoner vi har meldt ferdige."
  echo "Alle detaljer står i [testrapporten](specs/index.md)."
  for section in $sections; do
    lines="$(while read -r page; do
      if [[ "$(section_of "${page%%/*}")" == "$section" ]]; then rule_line "$page"; fi
    done <<<"$pages")"
    if [[ -z "$lines" ]]; then continue; fi
    echo
    echo "## $section"
    echo
    echo "$lines"
  done
}

write_page > "$out.tmp" && mv "$out.tmp" "$out"
echo "Wrote $out" >&2
