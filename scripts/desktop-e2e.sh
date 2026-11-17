#!/usr/bin/env bash
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESULTS_DIR="$ROOT_DIR/.allure/desktop/results"
REPORTS_DIR="$ROOT_DIR/.allure/desktop/reports"

rm -rf "$RESULTS_DIR" "$REPORTS_DIR"

"$ROOT_DIR/gradlew" -p "$ROOT_DIR" :desktopApp:e2eTest --rerun "$@"
status=$?

allure generate "$RESULTS_DIR" -o "$REPORTS_DIR"

exit $status
