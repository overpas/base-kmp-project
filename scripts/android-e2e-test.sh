#!/usr/bin/env bash
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/device-lock.sh"
source "$(dirname "${BASH_SOURCE[0]}")/android-emulator.sh"
CAMERA_RELAY="$(dirname "${BASH_SOURCE[0]}")/../core/ui-test/camera/scripts/camera-relay.sh"
if [[ -f "$CAMERA_RELAY" ]]; then
    source "$CAMERA_RELAY"
else
    start_camera_relay() { return 1; }
    stop_camera_relay() { :; }
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR="${APP_DIR:-.}"
APP_ID="${APP_ID:-by.overpas.template}"
if [[ "$APP_DIR" == "." ]]; then
    GRADLE_PROJECT=":androidApp"
else
    GRADLE_PROJECT=":${APP_DIR//\//:}:androidApp"
fi
RESULTS_DIR="$ROOT_DIR/$APP_DIR/.allure/android/results"
REPORTS_DIR="$ROOT_DIR/$APP_DIR/.allure/android/reports"
CAMERA_DIR="$ROOT_DIR/$APP_DIR/androidApp/src/androidTest/camera"
CAMERA_MODULE_DIR="$ROOT_DIR/core/ui-test/camera"
CAMERA_TEST_DIR="$CAMERA_MODULE_DIR/src/androidDeviceTest/camera"
GALLERY_MODULE_DIR="$ROOT_DIR/core/ui-test/gallery"
GRPC_PORT="${E2E_GRPC_PORT:-$((E2E_EMULATOR_PORT + 3000))}"

cd "$ROOT_DIR" || exit 1

if [[ -z "${ANDROID_SERIAL:-}" ]]; then
    trap 'stop_camera_relay; stop_emulator; release_device_lock' EXIT
    start_emulator || exit 1
else
    trap 'stop_camera_relay' EXIT
fi

# Clear old allure artifacts
rm -rf "$RESULTS_DIR" "$REPORTS_DIR"
mkdir -p "$RESULTS_DIR"
# Clear the app's data directory
if ! adb shell pm path "$APP_ID" > /dev/null 2>&1; then
    ./gradlew "$GRADLE_PROJECT:installDebug" || exit 1
fi
initial_boot_id="$(boot_id)"
if ! adb shell pm clear "$APP_ID" > /dev/null 2>&1; then
    echo "Could not clear the data of $APP_ID" >&2
    exit 1
fi
if [[ -n "$initial_boot_id" ]]; then
    for _ in 1 2 3 4 5; do
        sleep 1
        [[ "$(boot_id)" == "$initial_boot_id" ]] || break
    done
    wait_for_boot || exit 1
fi
if [[ -d "$CAMERA_DIR" && -d "$CAMERA_MODULE_DIR" ]] && ! start_camera_relay "$CAMERA_DIR" "$GRPC_PORT"; then
    echo "The camera relay did not start, so tests cannot choose a back camera image" >&2
fi
# Run tests
./gradlew "$GRADLE_PROJECT:connectedAndroidTest" -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true "$@"
status=$?
stop_camera_relay
# Load allure artifacts
adb exec-out run-as "$APP_ID" tar -c -C "/data/data/$APP_ID/files" allure-results | tar -x -C "$RESULTS_DIR" --strip-components=1
# Generate allure report
allure generate "$RESULTS_DIR" -o "$REPORTS_DIR"
if [[ -d "$CAMERA_DIR" && -d "$CAMERA_MODULE_DIR" ]]; then
    if ! start_camera_relay "$CAMERA_TEST_DIR" "$GRPC_PORT"; then
        echo "The camera relay did not start for the camera test module" >&2
    fi
    ./gradlew :core:ui-test:camera:connectedAndroidDeviceTest || status=1
    stop_camera_relay
fi
if [[ "$APP_DIR" == "." && -d "$GALLERY_MODULE_DIR" ]]; then
    ./gradlew :core:ui-test:gallery:connectedAndroidDeviceTest || status=1
fi

exit $status
