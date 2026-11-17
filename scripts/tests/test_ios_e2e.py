import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


_ROOT = Path(__file__).resolve().parents[2]
_UDID = "11111111-2222-3333-4444-555555555555"

_XCRUN = f"""#!/bin/sh
echo "xcrun $*" >> "$FAKE_STATE/calls.log"
if [ "$*" = "simctl list devices available" ]; then
    echo "-- iOS 26.5 --"
    echo "    iPhone 17 Pro ({_UDID}) (Shutdown) "
fi
exit 0
"""

_LOGGER = """#!/bin/sh
echo "$(basename "$0") $*" >> "$FAKE_STATE/calls.log"
exit 0
"""

_OPEN_WITHOUT_DEVICE_HUB = """#!/bin/sh
case "$1" in devices://*) exit 1 ;; esac
echo "open $*" >> "$FAKE_STATE/calls.log"
exit 0
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
        self.bin = base / "bin"
        self.state.mkdir()
        (self.root / "scripts").mkdir(parents=True)
        for script in ("ios-e2e.sh", "device-lock.sh"):
            shutil.copy2(_ROOT / "scripts" / script, self.root / "scripts")
        self.gallery_scheme = self.root / "iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/GalleryTestHostUITests.xcscheme"
        self.gallery_scheme.parent.mkdir(parents=True)
        self.gallery_scheme.write_text("<Scheme/>", encoding="utf-8")
        _executable(self.bin / "xcrun", _XCRUN)
        for tool in ("xcodebuild", "open", "allure"):
            _executable(self.bin / tool, _LOGGER)

    def run(self, **env):
        variables = {
            "PATH": f"{self.bin}:/usr/bin:/bin",
            "HOME": os.environ.get("HOME", str(self.root)),
            "E2E_DEVICE_LOCK_HELD": "1",
            "FAKE_STATE": str(self.state),
            **env,
        }
        return subprocess.run(
            ["bash", str(self.root / "scripts/ios-e2e.sh")],
            env=variables,
            capture_output=True,
            text=True,
            timeout=60,
        )

    def calls(self):
        return (self.state / "calls.log").read_text(encoding="utf-8").splitlines()


class VisualModeTests(unittest.TestCase):

    def test_shows_its_device_in_device_hub_before_the_tests_in_visual_mode(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run(E2E_VISUAL="1")

            calls = [call.split(" ", 1)[0] for call in harness.calls()]
            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertIn(f"open devices://device/open?id={_UDID}", harness.calls())
            self.assertLess(calls.index("open"), calls.index("xcodebuild"))

    def test_shows_its_device_in_the_simulator_app_when_xcode_has_no_device_hub(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            _executable(harness.bin / "open", _OPEN_WITHOUT_DEVICE_HUB)

            completed = harness.run(E2E_VISUAL="1")

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertIn(f"open -a Simulator --args -CurrentDeviceUDID {_UDID}", harness.calls())

    def test_runs_the_tests_without_the_simulator_window_by_default(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run()

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertNotIn("open", [call.split(" ", 1)[0] for call in harness.calls()])


class GalleryTests(unittest.TestCase):

    def test_runs_the_gallery_tests_after_the_app_tests(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            completed = harness.run()

            schemes = [call.split("-scheme ", 1)[1].split(" ", 1)[0] for call in harness.calls() if call.startswith("xcodebuild")]
            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertEqual(["iosAppUITests", "GalleryTestHostUITests"], schemes)

    def test_runs_no_gallery_tests_for_another_app(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)

            harness.run(APP_DIR="sample", BUNDLE_ID="com.example.sample")

            self.assertNotIn("GalleryTestHostUITests", " ".join(harness.calls()))

    def test_runs_no_gallery_tests_when_the_gallery_scheme_is_removed(self):
        with tempfile.TemporaryDirectory() as directory:
            harness = _Harness(directory)
            harness.gallery_scheme.unlink()

            completed = harness.run()

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertNotIn("GalleryTestHostUITests", " ".join(harness.calls()))


if __name__ == "__main__":
    unittest.main()
