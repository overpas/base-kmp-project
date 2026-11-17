#!/usr/bin/env bash

CAMERA_RELAY_TAG="E2eCamera"
CAMERA_RELAY_PROPERTY="debug.e2e.camera"
CAMERA_RELAY_DEFAULT="default"
CAMERA_RELAY_RELOADED="00000000020801"

camera_relay_console() {
    adb emu "$@" 2>/dev/null | tr -d '\r' | sed -n '1{/^OK/d;/^KO/d;p;}'
}

enable_camera_grpc() {
    local port="$1"
    if [[ ! "$(camera_relay_console avd grpc)" =~ ^[0-9]+$ ]]; then
        adb emu grpc "$port" > /dev/null 2>&1
    fi
    local discovery
    discovery="$(camera_relay_console avd discoverypath)"
    CAMERA_GRPC_PORT="$(sed -n 's/^grpc\.port=//p' "$discovery" 2>/dev/null)"
    CAMERA_GRPC_TOKEN="$(sed -n 's/^grpc\.token=//p' "$discovery" 2>/dev/null)"
    CAMERA_AVD_DIR="$(camera_relay_console avd path)"
    if [[ -z "$CAMERA_GRPC_PORT" || -z "$CAMERA_GRPC_TOKEN" ]]; then
        echo "The emulator gRPC endpoint is not available: ${discovery:-no discovery file}" >&2
        return 1
    fi
    if [[ ! -f "$CAMERA_AVD_DIR/environment.ini" ]]; then
        echo "The emulator did not boot with an environment.ini: ${CAMERA_AVD_DIR:-no AVD path}" >&2
        return 1
    fi
}

show_camera_image() {
    local image="$1"
    local environment
    if [[ "$image" == "$CAMERA_RELAY_DEFAULT" ]]; then
        environment="$CAMERA_RELAY_BOOT_ENVIRONMENT"
    elif [[ "$image" =~ ^[A-Za-z0-9._-]+$ && -f "$CAMERA_RELAY_DIR/$image" ]]; then
        environment="scene.mode = imagefile:$CAMERA_RELAY_DIR/$image"
    else
        echo "No camera image $image in $CAMERA_RELAY_DIR" >&2
        return 1
    fi
    printf '%s\n' "$environment" > "$CAMERA_AVD_DIR/environment.ini"
    local reply
    reply="$(printf '\0\0\0\0\0' | curl -s --max-time 10 --http2-prior-knowledge -X POST \
        -H "content-type: application/grpc" -H "te: trailers" -H "authorization: Bearer $CAMERA_GRPC_TOKEN" \
        --data-binary @- "http://127.0.0.1:$CAMERA_GRPC_PORT/android.emulation.control.incubating.VirtualSceneService/reloadEnvironment" |
        xxd -p | tr -d '\n')"
    [[ "$reply" == "$CAMERA_RELAY_RELOADED" ]]
}

camera_relay_loop() {
    local request image outcome
    while read -r request image; do
        [[ "$request" =~ ^[0-9a-f-]{36}$ ]] || continue
        outcome="failed"
        show_camera_image "$image" && outcome="shown"
        adb shell setprop "$CAMERA_RELAY_PROPERTY" "$request:$outcome" > /dev/null 2>&1
    done
}

start_camera_relay() {
    CAMERA_RELAY_DIR="$(cd "$1" && pwd)" || return 1
    enable_camera_grpc "$2" || return 1
    CAMERA_RELAY_BOOT_ENVIRONMENT="$(cat "$CAMERA_AVD_DIR/environment.ini")"
    local since
    since="$(adb shell "date '+%m-%d %H:%M:%S.000'" | tr -d '\r')"
    (adb logcat -v raw -T "$since" -s "$CAMERA_RELAY_TAG:I" 2>/dev/null | camera_relay_loop) &
    CAMERA_RELAY_PID=$!
}

stop_camera_relay() {
    [[ -n "${CAMERA_RELAY_PID:-}" ]] || return 0
    pkill -P "$CAMERA_RELAY_PID" 2>/dev/null
    kill "$CAMERA_RELAY_PID" 2>/dev/null
    wait "$CAMERA_RELAY_PID" 2>/dev/null
    CAMERA_RELAY_PID=""
    show_camera_image "$CAMERA_RELAY_DEFAULT" > /dev/null 2>&1
}
