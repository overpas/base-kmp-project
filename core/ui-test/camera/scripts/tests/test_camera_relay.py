import os
from pathlib import Path
import subprocess
import tempfile
import time
import unittest


_RELAY = Path(__file__).resolve().parents[1] / "camera-relay.sh"
_FIRST = "11111111-1111-1111-1111-111111111111"
_SECOND = "22222222-2222-2222-2222-222222222222"
_RELOADED = "00000000020801"

_ADB = """#!/bin/sh
echo "$*" >> "$FAKE_STATE/adb.log"
case "$*" in
    "emu avd grpc")
        if [ -e "$FAKE_STATE/grpc-running" ]; then echo 8584; echo OK; else echo "KO: no gRPC endpoint"; fi
        ;;
    "emu grpc "*) : > "$FAKE_STATE/grpc-running"; echo OK ;;
    "emu avd discoverypath") echo "$FAKE_STATE/discovery.ini"; echo OK ;;
    "emu avd path") echo "$FAKE_AVD_DIR"; echo OK ;;
    "shell date "*) echo "10-24 12:00:00.000" ;;
    "logcat "*)
        cat "$FAKE_STATE/requests"
        if [ -n "$FAKE_LOGCAT_BLOCKS" ]; then exec sleep 31.5; fi
        ;;
    "shell setprop "*) echo "$4" >> "$FAKE_STATE/setprop.log" ;;
esac
exit 0
"""

_CURL = """#!/bin/sh
echo "$*" >> "$FAKE_STATE/curl.log"
cat > /dev/null
cat "$FAKE_AVD_DIR/environment.ini" >> "$FAKE_STATE/reloaded.log"
printf '%s' "$CURL_REPLY_HEX" | xxd -r -p
"""

_DRIVER = """
source "$RELAY"
start_camera_relay "$CAMERA_DIR" 8584 || exit 3
if [[ -n "${FAKE_LOGCAT_BLOCKS:-}" ]]; then
    until [[ -s "$FAKE_STATE/setprop.log" ]]; do sleep 0.05; done
else
    wait "$CAMERA_RELAY_PID"
fi
stop_camera_relay
"""


def _executable(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    path.chmod(0o755)


class _Harness:

    def __init__(self, directory):
        base = Path(directory)
        self.state = base / "state"
        self.bin = base / "bin"
        self.avd = base / "avd/e2e.avd"
        self.camera = base / "camera"
        self.state.mkdir()
        self.avd.mkdir(parents=True)
        self.camera.mkdir()
        _executable(self.bin / "adb", _ADB)
        _executable(self.bin / "curl", _CURL)
        (self.state / "discovery.ini").write_text("grpc.port=8584\ngrpc.token=secret-token\n", encoding="utf-8")
        (self.state / "requests").write_text("", encoding="utf-8")
        self.boot_line = "scene.mode = imagefile:/avd/camera-back.png"
        (self.avd / "environment.ini").write_text(f"{self.boot_line}\n", encoding="utf-8")

    def add_image(self, name):
        (self.camera / name).write_bytes(b"\x89PNG")

    def request(self, *lines):
        (self.state / "requests").write_text("".join(f"{line}\n" for line in lines), encoding="utf-8")

    def run(self, **env):
        variables = {
            "PATH": f"{self.bin}:/usr/bin:/bin",
            "HOME": os.environ.get("HOME", str(self.state)),
            "FAKE_STATE": str(self.state),
            "FAKE_AVD_DIR": str(self.avd),
            "RELAY": str(_RELAY),
            "CAMERA_DIR": str(self.camera),
            "CURL_REPLY_HEX": _RELOADED,
            **env,
        }
        return subprocess.run(
            ["bash", "-c", _DRIVER],
            env={name: value for name, value in variables.items() if value is not None},
            capture_output=True,
            text=True,
            timeout=20,
        )

    def lines(self, name):
        path = self.state / name
        return path.read_text(encoding="utf-8").splitlines() if path.exists() else []

    def environment(self):
        return (self.avd / "environment.ini").read_text(encoding="utf-8")


class CameraRelayRequestTests(unittest.TestCase):

    def test_shows_the_requested_image_before_it_acknowledges_the_request(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            harness.add_image("switch.png")
            harness.request(f"{_FIRST} switch.png")

            completed = harness.run()

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertEqual(f"scene.mode = imagefile:{harness.camera}/switch.png", harness.lines("reloaded.log")[0])
            self.assertEqual([f"{_FIRST}:shown"], harness.lines("setprop.log"))

    def test_reloads_the_environment_with_the_token_of_the_emulator(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            harness.add_image("switch.png")
            harness.request(f"{_FIRST} switch.png")

            harness.run()

            call = harness.lines("curl.log")[0]
            self.assertIn("authorization: Bearer secret-token", call)
            self.assertIn(
                "http://127.0.0.1:8584/android.emulation.control.incubating.VirtualSceneService/reloadEnvironment",
                call,
            )

    def test_reports_a_failure_when_the_emulator_does_not_reload_the_image(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            harness.add_image("switch.png")
            harness.request(f"{_FIRST} switch.png")

            harness.run(CURL_REPLY_HEX="0000000000")

            self.assertEqual([f"{_FIRST}:failed"], harness.lines("setprop.log"))

    def test_refuses_an_image_outside_the_camera_directory(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            harness.request(f"{_FIRST} ../state/discovery.ini")

            harness.run()

            self.assertEqual([f"{_FIRST}:failed"], harness.lines("setprop.log"))
            self.assertEqual([harness.boot_line], harness.lines("reloaded.log"))

    def test_refuses_a_missing_image(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            harness.request(f"{_FIRST} missing.png")

            harness.run()

            self.assertEqual([f"{_FIRST}:failed"], harness.lines("setprop.log"))
            self.assertEqual([harness.boot_line], harness.lines("reloaded.log"))

    def test_shows_the_boot_image_for_the_default_request(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            harness.add_image("switch.png")
            harness.request(f"{_FIRST} switch.png", f"{_SECOND} default")

            harness.run()

            self.assertEqual([f"{_FIRST}:shown", f"{_SECOND}:shown"], harness.lines("setprop.log"))
            self.assertEqual(harness.boot_line, harness.lines("reloaded.log")[1])

    def test_ignores_lines_that_are_not_requests(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            harness.add_image("switch.png")
            harness.request("--------- beginning of main", "switch.png", f"{_FIRST} switch.png")

            harness.run()

            self.assertEqual([f"{_FIRST}:shown"], harness.lines("setprop.log"))


class CameraRelayLifecycleTests(unittest.TestCase):

    def test_starts_the_grpc_endpoint_when_the_emulator_has_none(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            harness.run()

            self.assertIn("emu grpc 8584", harness.lines("adb.log"))

    def test_keeps_the_grpc_endpoint_that_the_emulator_already_has(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            (harness.state / "grpc-running").write_text("", encoding="utf-8")

            harness.run()

            self.assertNotIn("emu grpc 8584", harness.lines("adb.log"))

    def test_reads_only_requests_logged_after_it_starts(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            harness.run()

            self.assertIn("logcat -v raw -T 10-24 12:00:00.000 -s E2eCamera:I", harness.lines("adb.log"))

    def test_does_not_start_when_the_emulator_booted_without_an_environment(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            (harness.avd / "environment.ini").unlink()

            completed = harness.run()

            self.assertEqual(3, completed.returncode)
            self.assertIn("The emulator did not boot with an environment.ini", completed.stderr)

    def test_does_not_start_without_a_grpc_token(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            (harness.state / "discovery.ini").write_text("grpc.port=8584\n", encoding="utf-8")

            completed = harness.run()

            self.assertEqual(3, completed.returncode)
            self.assertIn("The emulator gRPC endpoint is not available", completed.stderr)

    def test_shows_the_boot_image_again_when_it_stops(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            harness.add_image("switch.png")
            harness.request(f"{_FIRST} switch.png")

            harness.run()

            self.assertEqual(f"{harness.boot_line}\n", harness.environment())

    def test_stops_its_logcat_reader_when_it_stops(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            harness.add_image("switch.png")
            harness.request(f"{_FIRST} switch.png")
            started = time.monotonic()

            completed = harness.run(FAKE_LOGCAT_BLOCKS="1")

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertLess(time.monotonic() - started, 10)
            self.assertNotEqual(0, subprocess.run(["pgrep", "-f", "sleep 31.5"], capture_output=True).returncode)


if __name__ == "__main__":
    unittest.main()
