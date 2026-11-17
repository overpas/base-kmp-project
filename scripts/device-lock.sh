#!/usr/bin/env bash

release_device_lock() {
    if [[ -n "${DEVICE_LOCK_DIR:-}" ]]; then
        rm -rf "$DEVICE_LOCK_DIR"
    fi
}

if [[ -n "${E2E_DEVICE_LOCK_HELD:-}" ]]; then
    return 0
fi

DEVICE_LOCK_DIR="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --path-format=absolute --git-common-dir)/e2e.lock"

until mkdir "$DEVICE_LOCK_DIR" 2>/dev/null; do
    lock_pid="$(cat "$DEVICE_LOCK_DIR/pid" 2>/dev/null)"
    if [[ -n "$lock_pid" ]] && ! kill -0 "$lock_pid" 2>/dev/null; then
        rm -rf "$DEVICE_LOCK_DIR"
        continue
    fi
    echo "Waiting for the e2e device lock held by process ${lock_pid:-unknown}: $DEVICE_LOCK_DIR" >&2
    sleep 10
done
echo $$ > "$DEVICE_LOCK_DIR/pid"
trap release_device_lock EXIT
