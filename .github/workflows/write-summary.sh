#!/bin/bash
# Usage: write-summary.sh <SERVICE_NAME> <SERVICE_DIR> <SONAR_PROJECT_KEY>
SERVICE_NAME="$1"
SERVICE_DIR="$2"
SONAR_KEY="$3"

# ── Unit Test Results ──────────────────────────────────────────────────────────
SUREFIRE_DIR="${SERVICE_DIR}/target/surefire-reports"
TESTS_TOTAL=0; TESTS_FAILED=0; TESTS_SKIPPED=0; TESTS_PASSED=0
if [ -d "$SUREFIRE_DIR" ]; then
  for xml in "$SUREFIRE_DIR"/*.xml; do
    [ -f "$xml" ] || continue
    t=$(grep -oP 'tests="\K[0-9]+' "$xml" | head -1); TESTS_TOTAL=$((TESTS_TOTAL + ${t:-0}))
    f=$(grep -oP 'failures="\K[0-9]+' "$xml" | head -1); TESTS_FAILED=$((TESTS_FAILED + ${f:-0}))
    e=$(grep -oP 'errors="\K[0-9]+' "$xml" | head -1); TESTS_FAILED=$((TESTS_FAILED + ${e:-0}))
    s=$(grep -oP 'skipped="\K[0-9]+' "$xml" | head -1); TESTS_SKIPPED=$((TESTS_SKIPPED + ${s:-0}))
  done
  TESTS_PASSED=$((TESTS_TOTAL - TESTS_FAILED - TESTS_SKIPPED))
fi
if [ "$TESTS_TOTAL" -gt 0 ] && [ "$TESTS_FAILED" -eq 0 ]; then
  TEST_STATUS="✅ Passed"
elif [ "$TESTS_TOTAL" -gt 0 ]; then
  TEST_STATUS="⚠️ Some Failed"
else
  TEST_STATUS="❌ No Tests Run"
fi

# ── JaCoCo Coverage ────────────────────────────────────────────────────────────
JACOCO_XML="${SERVICE_DIR}/target/site/jacoco/jacoco.xml"
COVERAGE_PCT="N/A"
if [ -f "$JACOCO_XML" ]; then
  MISSED=$(grep -oP 'type="INSTRUCTION"[^/]*/>' "$JACOCO_XML" | grep -oP 'missed="\K[0-9]+' | awk '{s+=$1} END{print s}')
  COVERED=$(grep -oP 'type="INSTRUCTION"[^/]*/>' "$JACOCO_XML" | grep -oP 'covered="\K[0-9]+' | awk '{s+=$1} END{print s}')
  TOTAL_INS=$((MISSED + COVERED))
  if [ "$TOTAL_INS" -gt 0 ]; then
    COVERAGE_PCT=$(awk "BEGIN {printf \"%.1f\", ($COVERED/$TOTAL_INS)*100}")%
  fi
fi
if [[ "$COVERAGE_PCT" == "N/A" ]]; then
  COV_STATUS="❌ Not Generated"
elif (( $(echo "${COVERAGE_PCT%\%} >= 70" | bc -l) )); then
  COV_STATUS="✅ ${COVERAGE_PCT}"
elif (( $(echo "${COVERAGE_PCT%\%} >= 40" | bc -l) )); then
  COV_STATUS="⚠️ ${COVERAGE_PCT}"
else
  COV_STATUS="❌ ${COVERAGE_PCT}"
fi

# ── Snyk Scan ──────────────────────────────────────────────────────────────────
if [ -f "snyk-report.json" ]; then
  HIGH=$(python3 -c "import json,sys; d=json.load(open('snyk-report.json')); vulns=d.get('vulnerabilities',[]) if isinstance(d,dict) else []; print(len([v for v in vulns if v.get('severity')=='high']))" 2>/dev/null || echo "?")
  CRITICAL=$(python3 -c "import json,sys; d=json.load(open('snyk-report.json')); vulns=d.get('vulnerabilities',[]) if isinstance(d,dict) else []; print(len([v for v in vulns if v.get('severity')=='critical']))" 2>/dev/null || echo "?")
  if [ "$CRITICAL" = "0" ] && [ "$HIGH" = "0" ]; then
    SNYK_STATUS="✅ No High/Critical Issues"
  else
    SNYK_STATUS="⚠️ Critical: ${CRITICAL}, High: ${HIGH}"
  fi
else
  SNYK_STATUS="❌ Report Not Found"
fi

# ── SonarCloud ─────────────────────────────────────────────────────────────────
SONAR_URL="https://sonarcloud.io/project/overview?id=${SONAR_KEY}"
SONAR_STATUS="🔗 [View on SonarCloud](${SONAR_URL})"

# ── Write GitHub Step Summary ──────────────────────────────────────────────────
cat >> "$GITHUB_STEP_SUMMARY" << EOF

## 📊 ${SERVICE_NAME} — CI/CD Quality Summary

| Check | Status | Details |
|-------|--------|---------|
| 🧪 **Unit Tests** | ${TEST_STATUS} | Total: ${TESTS_TOTAL} · Passed: ${TESTS_PASSED} · Failed: ${TESTS_FAILED} · Skipped: ${TESTS_SKIPPED} |
| 📈 **Code Coverage** | ${COV_STATUS} | JaCoCo instruction coverage |
| 🛡️ **Snyk Security** | ${SNYK_STATUS} | Dependency vulnerability scan |
| 🔬 **SonarQube** | ${SONAR_STATUS} | Code quality gate |
| 🚀 **Lambda Deploy** | ✅ Triggered | AWS Lambda update via S3 |

> 🔗 SonarCloud Project: [${SONAR_KEY}](${SONAR_URL})
EOF

echo "✅ Summary written for ${SERVICE_NAME}"
