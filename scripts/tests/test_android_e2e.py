import os
from pathlib import Path
import platform
import shutil
import subprocess
import tempfile
import unittest


_ROOT = Path(__file__).resolve().parents[2]
_ABI = "arm64-v8a" if platform.machine() in ("arm64", "aarch64") else "x86_64"
_SYSTEM_IMAGE = f"system-images/android-36.1/google_apis_playstore/{_ABI}"

_ADB = """#!/bin/sh
if [ "$1" = "-s" ]; then shift 2; fi
echo "${ANDROID_SERIAL:-}|$*" >> "$FAKE_STATE/adb.log"
booted() { [ -e "$FAKE_STATE/booted" ] && [ ! -e "$FAKE_STATE/stopped" ]; }
case "$*" in
    "devices")
        echo "List of devices attached"
        if booted; then printf 'emulator-5584\\tdevice\\n'; fi
        ;;
    "shell cat /proc/sys/kernel/random/boot_id")
        if [ -e "$FAKE_STATE/boot-id-read" ]; then
            if [ -n "$ADB_LATER_BOOT_ID" ]; then echo "$ADB_LATER_BOOT_ID"; fi
        else
            : > "$FAKE_STATE/boot-id-read"
            echo "initial"
        fi
        ;;
    "get-state")
        if [ -n "$ADB_STATE" ]; then echo "$ADB_STATE"; elif booted; then echo device; else exit 1; fi
        ;;
    "shell pm path "*) if [ -n "$ADB_PACKAGE_MISSING" ] && [ ! -e "$FAKE_STATE/installed" ]; then exit 1; fi ;;
    "shell pm clear "*) if [ -n "$ADB_CLEAR_FAILS" ]; then echo Failed; exit 1; fi ;;
    "shell getprop sys.boot_completed") echo 1 ;;
    "shell reboot -p" | "emu kill") : > "$FAKE_STATE/stopped" ;;
    "emu avd grpc") if [ -e "$FAKE_STATE/grpc-running" ]; then echo 8584; fi ;;
    "emu grpc "*) : > "$FAKE_STATE/grpc-running" ;;
    "emu avd discoverypath") if [ -n "$FAKE_GRPC" ]; then echo "$FAKE_STATE/discovery.ini"; fi ;;
    "emu avd path") echo "$E2E_AVD_HOME/e2e.avd" ;;
    "shell date "*) echo "10-24 12:00:00.000" ;;
esac
exit 0
"""

_EMULATOR = """#!/bin/sh
if [ "$1" = "-help-camera-back" ]; then
    echo "environment -> camera will render a virtual scene"
    exit 0
fi
echo "$*" > "$FAKE_STATE/emulator-arguments"
echo "$ANDROID_AVD_HOME" > "$FAKE_STATE/emulator-avd-home"
if [ -n "$EMULATOR_EXITS" ]; then exit 1; fi
: > "$FAKE_STATE/booted"
until [ -e "$FAKE_STATE/stopped" ]; do sleep 0.1; done
exit 0
"""

_ANDROID = """#!/bin/sh
echo "$*" > "$FAKE_STATE/android-arguments"
mkdir -p "$FAKE_SDK/$FAKE_SYSTEM_IMAGE"
: > "$FAKE_SDK/$FAKE_SYSTEM_IMAGE/system.img"
"""

_CURL = """#!/bin/sh
echo "$*" >> "$FAKE_STATE/curl.log"
cat > /dev/null
printf '00000000020801' | xxd -r -p
"""

_GRADLEW = """#!/bin/sh
echo "$1" >> "$FAKE_STATE/gradle.log"
echo "${ANDROID_SERIAL:-}" >> "$FAKE_STATE/gradle-serial.log"
case "$1" in
    *:installDebug) : > "$FAKE_STATE/installed" ;;
esac
if [ "$1" = "${GRADLEW_FAILING_TASK:-}" ]; then exit 1; fi
exit "${GRADLEW_EXIT:-0}"
"""


def _executable(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    path.chmod(0o755)


class _Harness:

    def __init__(self, directory):
        base = Path(directory)
        self.root = base / "repo"
        self.state = base / "state"
        self.sdk = base / "sdk"
        self.avd_home = base / "avd"
        self.bin = base / "bin"
        self.state.mkdir()
        (self.root / "scripts").mkdir(parents=True)
        for script in ("android-e2e-test.sh", "android-emulator.sh", "device-lock.sh"):
            shutil.copy2(_ROOT / "scripts" / script, self.root / "scripts")
        relay = "core/ui-test/camera/scripts/camera-relay.sh"
        (self.root / relay).parent.mkdir(parents=True)
        shutil.copy2(_ROOT / relay, self.root / relay)
        _executable(self.root / "gradlew", _GRADLEW)
        _executable(self.bin / "allure", "#!/bin/sh\nexit 0\n")
        _executable(self.bin / "xcrun", "#!/bin/sh\nexit 0\n")
        _executable(self.bin / "adb", _ADB)
        _executable(self.bin / "curl", _CURL)
        _executable(self.sdk / "platform-tools/adb", _ADB)
        _executable(self.sdk / "emulator/emulator", _EMULATOR)
        (self.root / "local.properties").write_text(f"sdk.dir={self.sdk}\n", encoding="utf-8")
        self.add_system_image()
        self.add_camera_image("androidApp/src/androidTest/camera/back.png")

    def add_system_image(self):
        image = self.sdk / _SYSTEM_IMAGE / "system.img"
        image.parent.mkdir(parents=True, exist_ok=True)
        image.write_bytes(b"")

    def add_camera_image(self, relative):
        image = self.root / relative
        image.parent.mkdir(parents=True, exist_ok=True)
        image.write_bytes(b"\x89PNG")
        return image

    def run(self, **env):
        variables = {
            "PATH": f"{self.bin}:/usr/bin:/bin",
            "HOME": os.environ.get("HOME", str(self.root)),
            "E2E_DEVICE_LOCK_HELD": "1",
            "E2E_AVD_HOME": str(self.avd_home),
            "FAKE_STATE": str(self.state),
            "FAKE_SDK": str(self.sdk),
            "FAKE_SYSTEM_IMAGE": _SYSTEM_IMAGE,
            **env,
        }
        completed = subprocess.run(
            ["bash", str(self.root / "scripts/android-e2e-test.sh")],
            env={name: value for name, value in variables.items() if value is not None},
            capture_output=True,
            text=True,
            timeout=60,
        )
        return completed

    def lines(self, name):
        path = self.state / name
        return path.read_text(encoding="utf-8").splitlines() if path.exists() else []

    def text(self, name):
        return (self.state / name).read_text(encoding="utf-8")


class ReusedDeviceTests(unittest.TestCase):

    def test_runs_the_tests_after_the_device_reboots(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run(ANDROID_SERIAL="emulator-5554", ADB_LATER_BOOT_ID="rebooted", ADB_STATE="device")

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertEqual(
                [
                    ":androidApp:connectedAndroidTest",
                    ":core:ui-test:camera:connectedAndroidDeviceTest",
                    ":core:ui-test:gallery:connectedAndroidDeviceTest",
                ],
                harness.lines("gradle.log"),
            )

    def test_fails_without_the_tests_when_the_device_does_not_boot_in_time(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run(ANDROID_SERIAL="emulator-5554", ADB_STATE="offline", BOOT_TIMEOUT="2")

            self.assertEqual(1, completed.returncode)
            self.assertIn("The device did not finish booting in 2 s", completed.stderr)
            self.assertEqual([], harness.lines("gradle.log"))

    def test_installs_the_app_before_clearing_it_when_the_device_does_not_know_it(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run(ANDROID_SERIAL="emulator-5554", ADB_PACKAGE_MISSING="1", ADB_STATE="device")

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertEqual(
                [
                    ":androidApp:installDebug",
                    ":androidApp:connectedAndroidTest",
                    ":core:ui-test:camera:connectedAndroidDeviceTest",
                    ":core:ui-test:gallery:connectedAndroidDeviceTest",
                ],
                harness.lines("gradle.log"),
            )

    def test_fails_without_the_tests_when_the_app_data_cannot_be_cleared(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run(ANDROID_SERIAL="emulator-5554", ADB_CLEAR_FAILS="1", ADB_STATE="device")

            self.assertEqual(1, completed.returncode)
            self.assertIn("Could not clear the data of by.overpas.template", completed.stderr)
            self.assertEqual([], harness.lines("gradle.log"))

    def test_runs_on_the_given_device_without_an_emulator(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run(ANDROID_SERIAL="emulator-5554", ADB_STATE="device")

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertEqual(["emulator-5554", "emulator-5554", "emulator-5554"], harness.lines("gradle-serial.log"))
            self.assertFalse((harness.state / "emulator-arguments").exists())


class ManagedEmulatorTests(unittest.TestCase):

    def test_runs_the_tests_on_its_own_emulator(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run()

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertIn("-avd e2e -port 5584 -no-window", harness.text("emulator-arguments"))
            self.assertEqual(["emulator-5584", "emulator-5584", "emulator-5584"], harness.lines("gradle-serial.log"))

    def test_shows_its_emulator_in_a_window_in_visual_mode(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run(E2E_VISUAL="1")

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertIn("-avd e2e -port 5584 -gpu host -no-snapshot", harness.text("emulator-arguments"))

    def test_shows_the_camera_image_on_the_back_camera(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            harness.run()

            self.assertIn("-camera-back environment", harness.text("emulator-arguments"))
            self.assertEqual(
                f"scene.mode = imagefile:{harness.avd_home}/camera-back.png\n",
                (harness.avd_home / "e2e.avd/environment.ini").read_text(encoding="utf-8"),
            )

    def test_shows_the_given_camera_image(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            image = harness.add_camera_image("custom/QR.JPG")
            image.write_bytes(b"custom")

            harness.run(E2E_CAMERA_IMAGE=str(image))

            self.assertEqual(b"custom", (harness.avd_home / "camera-back.jpg").read_bytes())

    def test_starts_without_a_camera_image_when_the_app_has_none(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run(APP_DIR="sample", APP_ID="by.overpas.notes")

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertNotIn("-camera-back", harness.text("emulator-arguments"))
            self.assertFalse((harness.avd_home / "e2e.avd/environment.ini").exists())

    def test_lets_the_tests_choose_a_camera_image_on_its_own_emulator(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            (harness.state / "discovery.ini").write_text("grpc.port=8584\ngrpc.token=secret-token\n", encoding="utf-8")

            completed = harness.run(FAKE_GRPC="1")

            commands = [line.split("|", 1)[1] for line in harness.lines("adb.log")]
            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertIn("emu grpc 8584", commands)
            self.assertIn("logcat -v raw -T 10-24 12:00:00.000 -s E2eCamera:I", commands)

    def test_runs_the_tests_when_the_camera_relay_does_not_start(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run()

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertIn("The camera relay did not start", completed.stderr)
            self.assertEqual(
                [
                    ":androidApp:connectedAndroidTest",
                    ":core:ui-test:camera:connectedAndroidDeviceTest",
                    ":core:ui-test:gallery:connectedAndroidDeviceTest",
                ],
                harness.lines("gradle.log"),
            )

    def test_runs_the_camera_module_tests_with_their_own_camera_images(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            harness.add_camera_image("core/ui-test/camera/src/androidDeviceTest/camera/black.png")
            (harness.state / "discovery.ini").write_text("grpc.port=8584\ngrpc.token=secret-token\n", encoding="utf-8")

            completed = harness.run(FAKE_GRPC="1")

            commands = [line.split("|", 1)[1] for line in harness.lines("adb.log")]
            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertEqual(2, commands.count("logcat -v raw -T 10-24 12:00:00.000 -s E2eCamera:I"))
            self.assertIn(":core:ui-test:camera:connectedAndroidDeviceTest", harness.lines("gradle.log"))

    def test_fails_when_only_the_camera_module_tests_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run(GRADLEW_FAILING_TASK=":core:ui-test:camera:connectedAndroidDeviceTest")

            self.assertEqual(1, completed.returncode)

    def test_fails_when_only_the_gallery_module_tests_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run(GRADLEW_FAILING_TASK=":core:ui-test:gallery:connectedAndroidDeviceTest")

            self.assertEqual(1, completed.returncode)

    def test_starts_no_camera_relay_when_the_app_has_no_camera_images(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            harness.run(APP_DIR="sample", APP_ID="by.overpas.notes")

            commands = [line.split("|", 1)[1] for line in harness.lines("adb.log")]
            self.assertNotIn("emu avd grpc", commands)

    def test_runs_no_camera_module_tests_when_the_app_has_no_camera_images(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            harness.run(APP_DIR="sample", APP_ID="by.overpas.notes")

            self.assertEqual([":sample:androidApp:connectedAndroidTest"], harness.lines("gradle.log"))

    def test_fails_without_the_tests_when_the_given_camera_image_does_not_exist(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run(E2E_CAMERA_IMAGE="missing.png")

            self.assertEqual(1, completed.returncode)
            self.assertIn("The camera image missing.png does not exist", completed.stderr)
            self.assertEqual([], harness.lines("gradle.log"))

    def test_writes_the_avd_only_into_its_own_avd_home(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            harness.run()

            self.assertEqual(str(harness.avd_home), harness.text("emulator-avd-home").strip())
            self.assertIn(f"path={harness.avd_home}/e2e.avd\n", (harness.avd_home / "e2e.ini").read_text(encoding="utf-8"))
            self.assertIn(f"image.sysdir.1={_SYSTEM_IMAGE}/\n", (harness.avd_home / "e2e.avd/config.ini").read_text(encoding="utf-8"))

    def test_keeps_the_data_of_the_next_run_on_the_same_system_image(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            harness.run()
            (harness.state / "booted").unlink()
            (harness.state / "stopped").unlink()

            harness.run()

            self.assertNotIn("-wipe-data", harness.text("emulator-arguments"))

    def test_wipes_the_data_when_the_system_image_changes(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            (harness.avd_home / "e2e.avd").mkdir(parents=True)
            (harness.avd_home / "e2e.avd/system-image").write_text("system-images;android-35;google_apis;x86_64\n", encoding="utf-8")

            harness.run()

            self.assertIn("-wipe-data", harness.text("emulator-arguments"))

    def test_installs_a_missing_system_image_with_the_android_cli(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            shutil.rmtree(harness.sdk / "system-images")
            _executable(harness.bin / "android", _ANDROID)

            completed = harness.run()

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertEqual(f"sdk install --sdk={harness.sdk} {_SYSTEM_IMAGE}", harness.text("android-arguments").strip())

    def test_fails_with_the_install_command_when_the_system_image_is_missing(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            shutil.rmtree(harness.sdk / "system-images")

            completed = harness.run()

            self.assertEqual(1, completed.returncode)
            self.assertIn(f"Install it with: android sdk install {_SYSTEM_IMAGE}", completed.stderr)
            self.assertFalse((harness.state / "emulator-arguments").exists())

    def test_applies_the_test_settings(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            harness.run()

            settings = "\n".join(harness.lines("adb.log"))
            self.assertIn("settings put global animator_duration_scale 0", settings)
            self.assertIn("settings put secure show_ime_with_hard_keyboard 0", settings)

    def test_powers_its_emulator_off_after_pulling_the_allure_results_of_failed_tests(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run(GRADLEW_EXIT="1")

            commands = [line.split("|", 1)[1] for line in harness.lines("adb.log")]
            pull = next(index for index, command in enumerate(commands) if command.startswith("exec-out run-as"))
            self.assertEqual(1, completed.returncode)
            self.assertLess(pull, commands.index("shell reboot -p"))

    def test_fails_without_the_tests_when_the_emulator_stops_before_it_boots(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run(EMULATOR_EXITS="1")

            self.assertEqual(1, completed.returncode)
            self.assertIn("The emulator stopped before it booted", completed.stderr)
            self.assertEqual([], harness.lines("gradle.log"))

    def test_does_not_start_while_an_ios_simulator_runs(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            _executable(harness.bin / "xcrun", "#!/bin/sh\necho '    iPhone (11111111-2222-3333-4444-555555555555) (Booted)'\n")

            completed = harness.run()

            self.assertEqual(1, completed.returncode)
            self.assertIn("An iOS simulator is running", completed.stderr)
            self.assertFalse((harness.state / "emulator-arguments").exists())

    def test_leaves_a_process_alone_that_reuses_the_pid_of_its_old_emulator(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            stranger = subprocess.Popen(["sleep", "30"])
            harness.avd_home.mkdir(parents=True)
            (harness.avd_home / "emulator.pid").write_text(f"{stranger.pid}\n", encoding="utf-8")
            try:

                harness.run()

                self.assertIsNone(stranger.poll())
            finally:
                stranger.kill()
                stranger.wait()

    def test_releases_the_device_lock_after_stopping_its_emulator(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            subprocess.run(["git", "init", "-q", str(harness.root)], check=True)

            completed = harness.run(E2E_DEVICE_LOCK_HELD=None)

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertTrue((harness.state / "stopped").exists())
            self.assertFalse((harness.root / ".git/e2e.lock").exists())


if __name__ == "__main__":
    unittest.main()
