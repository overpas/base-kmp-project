import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest import mock


_SCRIPTS = Path(__file__).resolve().parents[2] / "scripts/feature-factory"
sys.path.insert(0, str(_SCRIPTS))

import capture


def _git(root, *arguments):
    subprocess.run(["git", "-C", str(root), *arguments], check=True, capture_output=True)


def _executable(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    path.chmod(0o755)


class ExportTests(unittest.TestCase):

    def test_demo_lands_in_the_main_checkout(self):
        with tempfile.TemporaryDirectory() as directory:
            main = Path(directory).resolve() / "main"
            main.mkdir()
            _git(main, "init", "-q", "-b", "develop")
            _git(main, "-c", "user.name=Test", "-c", "user.email=test@example.com", "commit", "-q", "--allow-empty", "-m", "Base")
            worktree = main / ".claude/worktrees/search"
            _git(main, "worktree", "add", "-q", "-b", "search", str(worktree))
            demo = worktree / "build/feature-factory/search/demo"
            demo.mkdir(parents=True)
            (demo / "index.html").write_text("<title>Search demo</title>\n", encoding="utf-8")

            completed = subprocess.run(
                [sys.executable, str(_SCRIPTS / "capture.py"), "export", str(demo), "--slug", "search"],
                cwd=worktree,
                capture_output=True,
                text=True,
                timeout=60,
            )

            result = json.loads(completed.stdout)
            self.assertEqual(str(main / ".demos/search/index.html"), result["path"])
            self.assertEqual("<title>Search demo</title>\n", (main / ".demos/search/index.html").read_text(encoding="utf-8"))


class StepShotTests(unittest.TestCase):

    def test_allure_step_screenshots_become_captioned_stills_in_step_order(self):
        with tempfile.TemporaryDirectory() as directory:
            results = Path(directory) / "results"
            out = Path(directory) / "media"
            results.mkdir()
            out.mkdir()
            (results / "a-attachment.jpeg").write_bytes(b"first")
            (results / "b-attachment.png").write_bytes(b"second")
            (results / "c-attachment.txt").write_text("log", encoding="utf-8")
            (results / "1-result.json").write_text(json.dumps({
                "name": "test",
                "attachments": [{"name": "logcat", "source": "c-attachment.txt", "type": "text/plain"}],
                "steps": [
                    {"name": "Click Add button", "steps": [{"name": "Click on 'add'", "attachments": []}], "attachments": [{"name": "Click Add button.jpeg", "source": "a-attachment.jpeg", "type": "image/jpeg"}]},
                    {"name": "Click Save button", "attachments": [{"name": "Click Save button.png", "source": "b-attachment.png", "type": "image/png"}]},
                ],
            }), encoding="utf-8")

            shots = capture.allure_step_shots(results, out, "after-list-android")

            self.assertEqual(
                [("after-list-android-step-01.jpeg", "Click Add button"), ("after-list-android-step-02.png", "Click Save button")],
                [(shot.name, caption) for shot, caption in shots],
            )
            self.assertEqual(b"second", (out / "after-list-android-step-02.png").read_bytes())

    def test_unreadable_allure_results_are_skipped(self):
        with tempfile.TemporaryDirectory() as directory:
            results = Path(directory) / "results"
            out = Path(directory) / "media"
            results.mkdir()
            out.mkdir()
            (results / "a-attachment.jpeg").write_bytes(b"shot")
            (results / "0-result.json").write_text("", encoding="utf-8")
            (results / "1-result.json").write_text(json.dumps({
                "name": "test",
                "steps": [{"name": "Click Add button", "attachments": [{"source": "a-attachment.jpeg", "type": "image/jpeg"}]}],
            }), encoding="utf-8")

            shots = capture.allure_step_shots(results, out, "after-android")

            self.assertEqual([("after-android-step-01.jpeg", "Click Add button")], [(shot.name, caption) for shot, caption in shots])

    def test_xcresult_screenshots_are_ordered_by_time_and_named_by_step(self):
        with tempfile.TemporaryDirectory() as directory:
            exported = Path(directory) / "exported"
            out = Path(directory) / "media"
            exported.mkdir()
            out.mkdir()
            (exported / "late.png").write_bytes(b"late")
            (exported / "early.jpeg").write_bytes(b"early")
            (exported / "manifest.json").write_text(json.dumps([{
                "testIdentifier": "AddOtpEntryE2eTest/testOtpCodeIsDisplayedAfterEntryIsAddedManually()",
                "attachments": [
                    {"exportedFileName": "late.png", "suggestedHumanReadableName": "Click Save button_0_0F6E2C1A-7B7D-4E43-9A7B-2B5C3E9D1A11.png", "timestamp": 20.0},
                    {"exportedFileName": "early.jpeg", "suggestedHumanReadableName": "Click Add button_0_9A1D3E5C-2B7B-4E43-9A7B-7D1A0F6E2C1A.jpeg", "timestamp": 10.0},
                ],
            }]), encoding="utf-8")

            shots = capture.xcresult_step_shots(exported, out, "after-list-ios")

            self.assertEqual(
                [("after-list-ios-step-01.jpeg", "Click Add button"), ("after-list-ios-step-02.png", "Click Save button")],
                [(shot.name, caption) for shot, caption in shots],
            )
            self.assertEqual(b"early", (out / "after-list-ios-step-01.jpeg").read_bytes())

    def test_step_shots_replace_the_video_stills(self):
        with tempfile.TemporaryDirectory() as directory:
            out = Path(directory)
            frame = out / "after-01.png"
            frame.write_bytes(b"frame")
            shot = out / "after-step-01.png"
            shot.write_bytes(b"shot")

            stills, captions = capture.use_step_shots([frame], [(shot, "Click Add button")])

            self.assertEqual([shot], stills)
            self.assertEqual({"after-step-01.png": "Click Add button"}, captions)
            self.assertFalse(frame.exists())

    def test_video_stills_stay_without_step_shots(self):
        with tempfile.TemporaryDirectory() as directory:
            frame = Path(directory) / "after-01.png"
            frame.write_bytes(b"frame")

            stills, captions = capture.use_step_shots([frame], [])

            self.assertEqual([frame], stills)
            self.assertEqual({}, captions)
            self.assertTrue(frame.exists())


class AndroidCaptureTests(unittest.TestCase):

    def test_records_on_the_managed_emulator_and_stops_it_after_the_test(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            order = base / "order.log"
            script = base / "android-emulator.sh"
            _executable(script, f'#!/bin/sh\necho "$1" >> {order}\nif [ "$1" = start ]; then echo emulator-5584; fi\n')
            _executable(base / "project/scripts/android-e2e-test.sh", f'#!/bin/sh\necho "e2e serial=$ANDROID_SERIAL lock=$E2E_DEVICE_LOCK_HELD" >> {order}\n')
            _executable(base / "bin/adb", "#!/bin/sh\nexit 0\n")
            _executable(base / "bin/xcrun", "#!/bin/sh\nexit 0\n")
            out = base / "media"
            out.mkdir()

            with mock.patch.object(capture, "EMULATOR_SCRIPT", script):
                result = capture.capture_android(base / "project", "by.overpas.template.e2e.ScanTest", out, "after", {"PATH": f"{base / 'bin'}:/bin:/usr/bin"})

            self.assertEqual((0, None, [], {}), result)
            self.assertEqual(["start", "e2e serial=emulator-5584 lock=1", "stop"], order.read_text(encoding="utf-8").splitlines())


@unittest.skipUnless(shutil.which("ffmpeg"), "ffmpeg is missing")
class MediaTests(unittest.TestCase):

    def test_video_is_trimmed_and_stills_are_extracted(self):
        with tempfile.TemporaryDirectory() as directory:
            out = Path(directory)
            raw = out / "after-raw.mp4"
            subprocess.run(
                ["ffmpeg", "-y", "-loglevel", "error", "-f", "lavfi", "-i", "testsrc=duration=4:size=1080x1920:rate=10", "-pix_fmt", "yuv420p", str(raw)],
                check=True,
            )

            video, stills = capture.finish_media(raw, out, "after", 2.0)

            duration = subprocess.run(
                ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(video)],
                check=True,
                capture_output=True,
                text=True,
            ).stdout
            width = subprocess.run(
                ["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width", "-of", "csv=p=0", str(video)],
                check=True,
                capture_output=True,
                text=True,
            ).stdout
            self.assertAlmostEqual(2.0, float(duration), delta=0.3)
            self.assertEqual("720", width.strip())
            self.assertIn("after-last.png", [still.name for still in stills])
            self.assertIn("after-01.png", [still.name for still in stills])
            self.assertFalse(raw.exists())


if __name__ == "__main__":
    unittest.main()
