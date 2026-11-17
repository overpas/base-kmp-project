#!/usr/bin/env bash
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/device-lock.sh"

DEVICE="${DEVICE:-iPhone 17 Pro}"
OS_VERSION="${OS_VERSION:-26.5}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR="${APP_DIR:-.}"
BUNDLE_ID="${BUNDLE_ID:-by.overpas.template.template}"
PROJECT_DIR="$ROOT_DIR/$APP_DIR"
RESULTS_DIR="$PROJECT_DIR/.allure/ios/results"
REPORTS_DIR="$PROJECT_DIR/.allure/ios/reports"

# Clear old allure artifacts
rm -rf "$RESULTS_DIR" "$REPORTS_DIR"
mkdir -p "$RESULTS_DIR"

UDID="$(xcrun simctl list devices available | awk -v runtime="-- iOS $OS_VERSION --" -v name="    $DEVICE (" '/^-- / { in_runtime = ($0 == runtime); next } in_runtime && index($0, name) == 1 { match($0, /\([0-9A-F-]{36}\)/); print substr($0, RSTART + 1, 36); exit }')"
if [[ -z "$UDID" ]]; then
    echo "No simulator \"$DEVICE\" on iOS $OS_VERSION. Install the iOS $OS_VERSION simulator runtime in Xcode > Settings > Components, or set DEVICE and OS_VERSION." >&2
    exit 1
fi

xcrun simctl boot "$UDID" 2>/dev/null || true
xcrun simctl bootstatus "$UDID"
if [[ "${E2E_VISUAL:-}" == "1" ]]; then
    open "devices://device/open?id=$UDID" 2>/dev/null || open -a Simulator --args -CurrentDeviceUDID "$UDID"
fi
xcrun simctl uninstall "$UDID" "$BUNDLE_ID" 2>/dev/null || true

xcodebuild test \
    -project "$PROJECT_DIR/iosApp/iosApp.xcodeproj" \
    -scheme iosAppUITests \
    -destination "platform=iOS Simulator,id=$UDID" \
    -resultBundlePath "$RESULTS_DIR/e2e.xcresult" \
    -only-testing:iosAppUITests \
    "$@"
status=$?

if [[ "$APP_DIR" == "." ]]; then
    xcodebuild test \
        -project "$PROJECT_DIR/iosApp/iosApp.xcodeproj" \
        -scheme GalleryTestHostUITests \
        -destination "platform=iOS Simulator,id=$UDID" \
        -resultBundlePath "$RESULTS_DIR/gallery.xcresult" \
        || status=1
fi

# Generate allure report
allure generate "$RESULTS_DIR/e2e.xcresult" -o "$REPORTS_DIR"

exit $status
