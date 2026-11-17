#!/usr/bin/env bash

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR="${APP_DIR:-.}"
BOOT_TIMEOUT="${BOOT_TIMEOUT:-300}"
E2E_EMULATOR_PORT="${E2E_EMULATOR_PORT:-5584}"
AVD_NAME="e2e"
TEST_SETTINGS=(
    "settings put global window_animation_scale 0"
    "settings put global transition_animation_scale 0"
    "settings put global animator_duration_scale 0"
    "settings put global heads_up_notifications_enabled 0"
    "settings put global hide_error_dialogs 1"
    "settings put global package_verifier_enable 0"
    "settings put global verifier_verify_adb_installs 0"
    "settings put secure show_ime_with_hard_keyboard 0"
    "settings put secure spell_checker_enabled 0"
    "settings put secure autofill_service null"
    "settings put system screen_off_timeout 2147483647"
    "svc power stayon true"
    "locksettings set-disabled true"
    "input keyevent KEYCODE_WAKEUP"
    "wm dismiss-keyguard"
)

boot_id() {
    adb shell cat /proc/sys/kernel/random/boot_id 2>/dev/null | tr -d '\r'
}

running() {
    local state
    state="$(ps -o stat= -p "$1" 2>/dev/null)"
    [[ -n "$state" && "$state" != Z* ]]
}

wait_for_boot() {
    local deadline=$((SECONDS + BOOT_TIMEOUT))
    until [[ "$(adb get-state 2>/dev/null)" == "device" && "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
        if [[ -n "${EMULATOR_PID:-}" ]] && ! running "$EMULATOR_PID"; then
            echo "The emulator stopped before it booted:" >&2
            tail -20 "$AVD_HOME/emulator.log" >&2
            return 1
        fi
        if ((SECONDS >= deadline)); then
            echo "The device did not finish booting in $BOOT_TIMEOUT s" >&2
            return 1
        fi
        sleep 1
    done
}

bounded() {
    local seconds="$1"
    shift
    "$@" &
    local pid=$!
    local deadline=$((SECONDS + seconds))
    while running "$pid"; do
        if ((SECONDS >= deadline)); then
            kill "$pid" 2>/dev/null
            wait "$pid" 2>/dev/null
            return 124
        fi
        sleep 0.2
    done
    wait "$pid"
}

wait_for_exit() {
    local deadline=$((SECONDS + $2))
    while running "$1" && ((SECONDS < deadline)); do
        sleep 1
    done
}

load_emulator_settings() {
    local sdk
    sdk="$(sed -n 's/^sdk\.dir=//p' "$ROOT_DIR/local.properties" 2>/dev/null | tail -1)"
    SDK_DIR="${sdk:-${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}}"
    if [[ -n "${E2E_AVD_HOME:-}" ]]; then
        AVD_HOME="$E2E_AVD_HOME"
    else
        AVD_HOME="$(git -C "$ROOT_DIR" rev-parse --path-format=absolute --git-common-dir)/e2e-avd"
    fi
    EMULATOR_SERIAL="emulator-$E2E_EMULATOR_PORT"
    export PATH="$SDK_DIR/platform-tools:$PATH"
}

host_abi() {
    case "$(uname -m)" in
        arm64 | aarch64) echo "arm64-v8a" ;;
        *) echo "x86_64" ;;
    esac
}

ensure_system_image() {
    local image="$1"
    local sysdir="$2"
    local package="${image//;//}"
    if [[ -f "$SDK_DIR/$sysdir/system.img" ]]; then
        return 0
    fi
    if command -v android > /dev/null 2>&1; then
        echo "Installing the system image $image" >&2
        android sdk install --sdk="$SDK_DIR" "$package" >&2
    fi
    if [[ ! -f "$SDK_DIR/$sysdir/system.img" ]]; then
        echo "The system image $image is missing. Install it with: android sdk install $package (or: sdkmanager \"$image\")" >&2
        return 1
    fi
}

camera_image() {
    local image="${E2E_CAMERA_IMAGE:-}"
    if [[ -z "$image" ]]; then
        image="$ROOT_DIR/$APP_DIR/androidApp/src/androidTest/camera/back.png"
        [[ -f "$image" ]] || return 0
    fi
    if [[ ! -f "$image" ]]; then
        echo "The camera image $image does not exist" >&2
        return 1
    fi
    case "$(echo "${image##*.}" | tr '[:upper:]' '[:lower:]')" in
        png | jpg | jpeg) echo "$image" ;;
        *)
            echo "The camera image must be a PNG or a JPEG file: $image" >&2
            return 1
            ;;
    esac
}

write_avd() {
    local platform="$1"
    local tag="$2"
    local abi="$3"
    local sysdir="$4"
    local avd_dir="$AVD_HOME/$AVD_NAME.avd"
    local cpu_arch="$abi"
    [[ "$abi" == "arm64-v8a" ]] && cpu_arch="arm64"
    local play_store="false"
    [[ "$tag" == *playstore* ]] && play_store="true"
    mkdir -p "$avd_dir"
    printf 'avd.ini.encoding=UTF-8\npath=%s\ntarget=%s\n' "$avd_dir" "$platform" > "$AVD_HOME/$AVD_NAME.ini"
    cat > "$avd_dir/config.ini" << EOF
avd.ini.encoding=UTF-8
image.sysdir.1=$sysdir
tag.id=$tag
abi.type=$abi
hw.cpu.arch=$cpu_arch
hw.cpu.ncore=4
hw.ramSize=4096
disk.dataPartition.size=6G
PlayStore.enabled=$play_store
hw.lcd.width=1080
hw.lcd.height=2400
hw.lcd.density=420
hw.keyboard=yes
hw.mainKeys=no
hw.gpu.enabled=yes
hw.gpu.mode=auto
hw.camera.back=emulated
hw.camera.front=emulated
hw.audioInput=no
hw.sdCard=no
EOF
}

apply_test_settings() {
    local commands
    commands="$(printf '%s; ' "${TEST_SETTINGS[@]}")"
    bounded 120 adb shell am wait-for-broadcast-idle > /dev/null 2>&1
    adb shell "$commands" > /dev/null 2>&1
}

start_emulator() {
    load_emulator_settings
    local emulator="$SDK_DIR/emulator/emulator"
    local image="${E2E_SYSTEM_IMAGE:-system-images;android-36.1;google_apis_playstore;$(host_abi)}"
    local platform tag abi camera
    IFS=';' read -r _ platform tag abi <<< "$image"
    local sysdir="system-images/$platform/$tag/$abi/"
    local avd_dir="$AVD_HOME/$AVD_NAME.avd"
    if [[ ! -x "$emulator" ]]; then
        echo "The Android emulator is missing in $SDK_DIR. Install it with: android sdk install emulator" >&2
        return 1
    fi
    if command -v xcrun > /dev/null 2>&1 && xcrun simctl list devices booted 2>/dev/null | grep -q "(Booted)"; then
        echo "An iOS simulator is running. Shut it down first: xcrun simctl shutdown all" >&2
        return 1
    fi
    camera="$(camera_image)" || return 1
    if [[ -n "$camera" ]] && ! "$emulator" -help-camera-back 2>/dev/null | grep -q "environment"; then
        echo "This emulator cannot show an image on the camera. Update it with: android sdk install emulator" >&2
        return 1
    fi
    stop_emulator
    if adb devices 2>/dev/null | grep -q "^$EMULATOR_SERIAL[[:space:]]"; then
        echo "$EMULATOR_SERIAL belongs to another emulator. Set E2E_EMULATOR_PORT to a free even port from 5554 to 5584" >&2
        return 1
    fi
    ensure_system_image "$image" "$sysdir" || return 1
    write_avd "$platform" "$tag" "$abi" "$sysdir"
    local display=(-no-window -gpu lavapipe)
    if [[ "${E2E_VISUAL:-}" == "1" ]]; then
        display=(-gpu host)
    fi
    local arguments=(-avd "$AVD_NAME" -port "$E2E_EMULATOR_PORT" "${display[@]}" -no-snapshot -no-boot-anim -no-audio -no-metrics -skip-adb-auth -netfast)
    if [[ "$(cat "$avd_dir/system-image" 2>/dev/null)" != "$image" ]]; then
        arguments+=(-wipe-data)
    fi
    if [[ -n "$camera" ]]; then
        local copy
        copy="$AVD_HOME/camera-back.$(echo "${camera##*.}" | tr '[:upper:]' '[:lower:]')"
        cp "$camera" "$copy"
        printf 'scene.mode = imagefile:%s\n' "$copy" > "$avd_dir/environment.ini"
        arguments+=(-camera-back environment)
    else
        rm -f "$avd_dir/environment.ini"
    fi
    echo "$image" > "$avd_dir/system-image"
    echo "Starting the emulator $EMULATOR_SERIAL from $AVD_HOME" >&2
    ANDROID_AVD_HOME="$AVD_HOME" ANDROID_SDK_ROOT="$SDK_DIR" ANDROID_HOME="$SDK_DIR" \
        "$emulator" "${arguments[@]}" > "$AVD_HOME/emulator.log" 2>&1 < /dev/null &
    EMULATOR_PID=$!
    echo "$EMULATOR_PID" > "$AVD_HOME/emulator.pid"
    export ANDROID_SERIAL="$EMULATOR_SERIAL"
    if ! wait_for_boot; then
        stop_emulator
        return 1
    fi
    apply_test_settings
}

stop_emulator() {
    load_emulator_settings
    local pid_file="$AVD_HOME/emulator.pid"
    [[ -f "$pid_file" ]] || return 0
    local pid
    pid="$(cat "$pid_file")"
    if ps -ww -o command= -p "$pid" 2>/dev/null | grep -q -- "-avd $AVD_NAME -port $E2E_EMULATOR_PORT "; then
        echo "Stopping the emulator $EMULATOR_SERIAL" >&2
        if [[ "$(adb -s "$EMULATOR_SERIAL" get-state 2>/dev/null)" == "device" ]]; then
            bounded 30 adb -s "$EMULATOR_SERIAL" shell sync > /dev/null 2>&1
            bounded 30 adb -s "$EMULATOR_SERIAL" shell reboot -p > /dev/null 2>&1
            wait_for_exit "$pid" 60
        fi
        if running "$pid"; then
            bounded 30 adb -s "$EMULATOR_SERIAL" emu kill > /dev/null 2>&1
            wait_for_exit "$pid" 30
        fi
        if running "$pid"; then
            kill -KILL "$pid" 2>/dev/null
        fi
        local deadline=$((SECONDS + 30))
        while adb devices 2>/dev/null | grep -q "^$EMULATOR_SERIAL[[:space:]]" && ((SECONDS < deadline)); do
            sleep 1
        done
    fi
    rm -f "$pid_file"
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
    set -uo pipefail
    case "${1:-}" in
        start) start_emulator && echo "$ANDROID_SERIAL" ;;
        stop) stop_emulator ;;
        *)
            echo "Usage: $0 start|stop" >&2
            exit 2
            ;;
    esac
fi
