#!/usr/bin/env bash
# Runs the retrieval evaluation set against the live backend.
#
# Why a shell script and not a JUnit test: the Gradle test JVM could not reach localhost:8080
# in this environment (ConnectException while curl to the same URL returned 200), so the JUnit
# version silently skipped. The measurement matters more than the harness it lives in.
# RetrievalEvaluation.java is kept for when that is sorted out.
#
# Expectations are hand-labeled from what the corpus text means - see retrieval-queries.md.
# Usage:  bash rag/src/test/resources/eval/run-evaluation.sh
set -uo pipefail

BASE="${RAG_BASE:-http://127.0.0.1:8080/api/rag/search}"

# query | minTopScore | expectedSource
ASSERTED=(
  "ECOG performance status 0 or 1|0.90|INCLUSION_CRITERION"
  "pregnant or breastfeeding|0.85|EXCLUSION_CRITERION"
  "brain metastases|0.80|EXCLUSION_CRITERION"
  "triple negative breast cancer|0.75|INCLUSION_CRITERION"
  "HER2 positive|0.60|INCLUSION_CRITERION"
  "measurable disease RECIST|0.60|INCLUSION_CRITERION"
  "adequate liver function|0.60|INCLUSION_CRITERION"
  # Expected source is EXCLUSION: prior-chemo limits are written as exclusions, so the correct
  # answer is a disqualifying match. Guards the exclusion-flag logic.
  "no prior chemotherapy|0.60|EXCLUSION_CRITERION"
)

# Known-weak. Reported, never asserted - lowering the bar would hide the weakness we want to
# measure an upgrade against.
TRACKED=(
  "trials studying a BRCA mutation"
  "recruiting trials I could join now"
)

if ! curl -sf --max-time 10 -o /dev/null "${BASE}?query=ping&maxTrials=1"; then
  echo "backend unreachable at ${BASE} - start it and retry"
  exit 2
fi

PARSER="$(dirname "${BASH_SOURCE[0]}")/parse_top_match.py"

probe() {  # $1 = query -> "score<TAB>source<TAB>text"
  curl -sG --max-time 60 "$BASE" \
    --data-urlencode "query=$1" --data-urlencode 'maxTrials=1' \
  | python3 "$PARSER"
}

fails=0
printf '\n%-34s %7s %6s  %-20s %s\n' "QUERY" "SCORE" "MIN" "SOURCE" "RESULT"
printf '%.0s─' {1..104}; printf '\n'

for row in "${ASSERTED[@]}"; do
  [[ "$row" =~ ^# ]] && continue
  IFS='|' read -r q minscore expsrc <<< "$row"
  IFS=$'\t' read -r score src _text <<< "$(probe "$q")"

  verdict="PASS"
  awk -v a="$score" -v b="$minscore" 'BEGIN{exit !(a<b)}' && verdict="FAIL score"
  [[ "$src" != "$expsrc" ]] && verdict="FAIL source(got $src)"
  [[ "$verdict" == PASS ]] || fails=$((fails+1))

  printf '%-34s %7s %6s  %-20s %s\n' "$q" "$score" "$minscore" "$src" "$verdict"
done

printf '\nTRACKED (not asserted - for measuring a model upgrade)\n'
printf '%.0s─' {1..104}; printf '\n'
for q in "${TRACKED[@]}"; do
  IFS=$'\t' read -r score src text <<< "$(probe "$q")"
  printf '%-34s %7s  %-20s %s\n' "$q" "$score" "$src" "$text"
done

printf '\n%d asserted failure(s)\n' "$fails"
exit $(( fails > 0 ? 1 : 0 ))
