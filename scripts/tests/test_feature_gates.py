import contextlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


_ROOT = Path(__file__).resolve().parents[2]
_GATES = _ROOT / "scripts/feature-factory/gates.py"
_DEVICE = "Test Phone"
_OS_VERSION = "99.0"
_UDID = "11111111-2222-3333-4444-555555555555"


_GRADLEW = """#!/bin/sh
echo "gradlew $*"
if [ -n "$GRADLEW_OUTPUT" ]; then cat "$GRADLEW_OUTPUT"; fi
sleep "${GRADLEW_SLEEP:-0}"
exit "${GRADLEW_EXIT:-0}"
"""

_E2E = """#!/bin/sh
echo "e2e $0 lock=${E2E_DEVICE_LOCK_HELD:-none} serial=${ANDROID_SERIAL:-none}"
if [ -n "$E2E_FAIL_ONCE" ] && [ ! -e "$E2E_FAIL_ONCE" ]; then
    : > "$E2E_FAIL_ONCE"
    echo "Test FAILED once"
    exit 1
fi
if [ "${E2E_EXIT:-0}" != "0" ]; then echo "AssertionError: expected entry"; fi
exit "${E2E_EXIT:-0}"
"""

_ADB = """#!/bin/sh
if [ "$1" = "devices" ]; then
    echo "List of devices attached"
    if [ -n "$ADB_DEVICE" ] && [ ! -e "$FAKE_STATE/killed" ]; then printf '%s\\t%s\\n' "$ADB_DEVICE" "${ADB_STATE:-device}"; fi
    exit 0
fi
case "$*" in
    *"emu kill"*) : > "$FAKE_STATE/killed" ;;
esac
exit 0
"""

_EMULATOR_SCRIPT = """#!/bin/sh
echo "$1" >> "$FAKE_STATE/emulator.log"
if [ "$1" = "start" ]; then
    if [ -n "$EMULATOR_START_FAILS" ]; then
        echo "Starting the emulator" >&2
        echo "The system image is missing" >&2
        exit 1
    fi
    echo "Starting the emulator" >&2
    echo "emulator-5584"
fi
exit 0
"""

_XCRUN = f"""#!/bin/sh
if [ "$1 $2" = "simctl list" ]; then
    echo "== Devices =="
    echo "-- iOS {_OS_VERSION} --"
    echo "    {_DEVICE} ({_UDID}) (Shutdown) "
fi
exit 0
"""

_FAILURE = """> Task :core:arch:detekt FAILED
/project/core/arch/src/commonMain/kotlin/Handler.kt:12:5: Function is too long [LongMethod]

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':core:arch:detekt'.
> Analysis failed with 1 weighted issues.

* Try:
> Run with --stacktrace option to get the stack trace.

BUILD FAILED in {duration}
"""


def _executable(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    path.chmod(0o755)


def _git(root, *arguments):
    return subprocess.run(["git", "-C", str(root), *arguments], check=True, capture_output=True, text=True).stdout


def _commit(root, message):
    _git(root, "add", "-A")
    _git(root, "commit", "-q", "-m", message)
    return _git(root, "rev-parse", "HEAD").strip()


def _write(root, relative, content):
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


@contextlib.contextmanager
def _project(tools=("allure", "xcodebuild", "xcrun", "ffmpeg")):
    with tempfile.TemporaryDirectory() as directory:
        base = Path(directory)
        root = base / "project"
        sdk = base / "sdk"
        bin_dir = base / "bin"
        state = base / "state"
        state.mkdir()
        bin_dir.mkdir()
        os.symlink(shutil.which("git"), bin_dir / "git")
        for tool in tools:
            _executable(bin_dir / tool, _XCRUN if tool == "xcrun" else "#!/bin/sh\nexit 0\n")
        _executable(sdk / "platform-tools/adb", _ADB)
        _executable(sdk / "emulator/emulator", "#!/bin/sh\nexit 0\n")
        root.mkdir()
        _git(root, "init", "-q", "-b", "develop")
        _git(root, "config", "user.name", "Test")
        _git(root, "config", "user.email", "test@example.com")
        _git(root, "config", "commit.gpgsign", "false")
        (root / "scripts/feature-factory").mkdir(parents=True)
        shutil.copy2(_GATES, root / "scripts/feature-factory/gates.py")
        _executable(root / "gradlew", _GRADLEW)
        _executable(root / "scripts/android-e2e-test.sh", _E2E)
        _executable(root / "scripts/ios-e2e.sh", _E2E)
        _executable(root / "scripts/desktop-e2e.sh", _E2E)
        _executable(root / "scripts/android-emulator.sh", _EMULATOR_SCRIPT)
        _write(root, ".gitignore", "build/\nlocal.properties\n")
        _write(root, "local.properties", f"sdk.dir={sdk}\n")
        _write(root, "config/detekt/detekt.yml", "style:\n  active: true\n")
        _write(root, "module/src/commonMain/kotlin/Feature.kt", "class Feature\n")
        _write(root, "module/src/commonTest/kotlin/FeatureTest.kt", "class FeatureTest\n")
        _write(root, "module/src/uiTest/kotlin/FeatureUiTest.kt", "class FeatureUiTest\n")
        _write(root, "docs/features/demo/business-spec.md", "# Demo\n")
        base_sha = _commit(root, "Base")
        env = {
            "HOME": os.environ.get("HOME", directory),
            "PATH": f"{bin_dir}:/bin",
            "FAKE_STATE": str(state),
            "DEVICE": _DEVICE,
            "OS_VERSION": _OS_VERSION,
        }
        yield root, base_sha, env


def _run(root, env, *arguments, script=None):
    completed = subprocess.run(
        [sys.executable, str(script or root / "scripts/feature-factory/gates.py"), *arguments],
        cwd=root,
        env=env,
        capture_output=True,
        text=True,
        timeout=120,
    )
    return completed.returncode, json.loads(completed.stdout.strip().splitlines()[-1])


def _gates(root, base_sha, env, *gates, allow=(), run="r1a1"):
    options = [option for pattern in allow for option in ("--allow", pattern)]
    code, started = _run(root, env, "start", "--base", base_sha, "--slug", "demo", "--run", run, *options, *gates)
    if code != 0:
        raise AssertionError(started)
    code, result = _run(root, env, "wait", script=root / started["dir"] / "gates.py")
    return result


def _by_id(result):
    return {gate["id"]: gate for gate in result["gates"]}


class IntegrityTests(unittest.TestCase):

    def test_committed_feature_change_passes(self):
        with _project() as (root, base_sha, env):
            _write(root, "module/src/commonMain/kotlin/Feature.kt", "class Feature(val name: String)\n")
            _commit(root, "Change the feature")

            result = _gates(root, base_sha, env, "integrity")

            self.assertEqual("pass", _by_id(result)["integrity"]["status"])

    def test_uncommitted_change_fails(self):
        with _project() as (root, base_sha, env):
            _write(root, "module/src/commonMain/kotlin/Feature.kt", "class Feature(val name: String)\n")
            _commit(root, "Change the feature")
            _write(root, "module/src/commonMain/kotlin/Draft.kt", "class Draft\n")

            result = _gates(root, base_sha, env, "integrity")

            self.assertIn("uncommitted changes", _by_id(result)["integrity"]["excerpt"])

    def test_no_commit_after_the_base_fails(self):
        with _project() as (root, base_sha, env):

            result = _gates(root, base_sha, env, "integrity")

            self.assertIn("no commits after the base", _by_id(result)["integrity"]["excerpt"])

    def test_protected_path_changes_fail(self):
        with _project() as (root, base_sha, env):
            _write(root, "config/detekt/detekt.yml", "style:\n  active: false\n")
            _write(root, ".claude/agents/coder.md", "---\nname: coder\n---\n")
            _write(root, "docs/features/demo/business-spec.md", "# Demo, changed\n")
            _write(root, "module/.gitignore", "*.kt\n")
            _commit(root, "Weaken the checks")

            excerpt = _by_id(_gates(root, base_sha, env, "integrity"))["integrity"]["excerpt"]

            self.assertEqual(
                [
                    "protected path changed: .claude/agents/coder.md",
                    "protected path changed: config/detekt/detekt.yml",
                    "protected path changed: docs/features/demo/business-spec.md",
                    "protected path changed: module/.gitignore",
                ],
                sorted(excerpt.splitlines()),
            )

    def test_allowed_protected_path_passes(self):
        with _project() as (root, base_sha, env):
            _write(root, "config/detekt/detekt.yml", "style:\n  active: true\n  MaxLineLength: 140\n")
            _commit(root, "Change the detekt config")

            result = _gates(root, base_sha, env, "integrity", allow=("config/detekt/**",))

            self.assertEqual("pass", _by_id(result)["integrity"]["status"])

    def test_suppressions_and_skipped_tests_fail(self):
        with _project() as (root, base_sha, env):
            _write(root, "module/src/commonMain/kotlin/Feature.kt", '@Suppress("LongMethod")\nclass Feature\n')
            _write(root, "module/src/commonTest/kotlin/FeatureTest.kt", "class FeatureTest {\n    @Ignore\n    fun check() = Unit\n}\n")
            _write(root, "iosApp/iosAppUITests/FeatureTest.swift", "throw XCTSkip(\"later\")\n")
            _write(root, "docs/notes.md", '@Suppress("Anything") is fine in docs\n')
            _commit(root, "Silence the checks")

            excerpt = _by_id(_gates(root, base_sha, env, "integrity"))["integrity"]["excerpt"]

            self.assertEqual(
                [
                    'forbidden line added in iosApp/iosAppUITests/FeatureTest.swift: throw XCTSkip("later")',
                    "forbidden line added in module/src/commonMain/kotlin/Feature.kt: @Suppress(\"LongMethod\")",
                    "forbidden line added in module/src/commonTest/kotlin/FeatureTest.kt: @Ignore",
                ],
                sorted(excerpt.splitlines()),
            )

    def test_deleted_or_renamed_tests_fail(self):
        with _project() as (root, base_sha, env):
            (root / "module/src/commonTest/kotlin/FeatureTest.kt").unlink()
            (root / "module/src/uiTest/kotlin/FeatureUiTest.kt").rename(root / "module/src/uiTest/kotlin/OtherUiTest.kt")
            _commit(root, "Drop the tests")

            excerpt = _by_id(_gates(root, base_sha, env, "integrity"))["integrity"]["excerpt"]

            self.assertEqual(
                [
                    "test file deleted: module/src/commonTest/kotlin/FeatureTest.kt",
                    "test file renamed: module/src/uiTest/kotlin/FeatureUiTest.kt",
                ],
                sorted(excerpt.splitlines()),
            )

    def test_added_baseline_fails(self):
        with _project() as (root, base_sha, env):
            _write(root, "module/detekt-baseline.xml", "<SmellBaseline/>\n")
            _commit(root, "Add a baseline")

            excerpt = _by_id(_gates(root, base_sha, env, "integrity"))["integrity"]["excerpt"]

            self.assertEqual("baseline file added: module/detekt-baseline.xml", excerpt)

    def test_gate_runner_comes_from_the_base_commit(self):
        with _project() as (root, base_sha, env):
            gates = root / "scripts/feature-factory/gates.py"
            gates.write_text(gates.read_text(encoding="utf-8") + "\nprint('changed on the branch')\n", encoding="utf-8")
            _commit(root, "Change the gate runner")

            result = _gates(root, base_sha, env, "integrity")

            copy = root / "build/feature-factory/demo/r1a1/gates.py"
            self.assertEqual(_GATES.read_text(encoding="utf-8"), copy.read_text(encoding="utf-8"))
            self.assertIn("protected path changed: scripts/feature-factory/gates.py", _by_id(result)["integrity"]["excerpt"])


class GradleGateTests(unittest.TestCase):

    def test_failure_reports_excerpt_and_exit_code(self):
        with _project() as (root, base_sha, env):
            output = root.parent / "failure.txt"
            output.write_text(_FAILURE.format(duration="3s"), encoding="utf-8")

            detekt = _by_id(_gates(root, base_sha, {**env, "GRADLEW_EXIT": "1", "GRADLEW_OUTPUT": str(output)}, "detekt"))["detekt"]

            self.assertEqual(("fail", 1), (detekt["status"], detekt["exitCode"]))
            self.assertIn("Execution failed for task ':core:arch:detekt'.", detekt["excerpt"])
            self.assertIn("Handler.kt:12:5: Function is too long [LongMethod]", detekt["excerpt"])

    def test_fingerprint_ignores_durations(self):
        with _project() as (root, base_sha, env):
            first = root.parent / "first.txt"
            first.write_text(_FAILURE.format(duration="3s"), encoding="utf-8")
            second = root.parent / "second.txt"
            second.write_text(_FAILURE.format(duration="1m 7s"), encoding="utf-8")

            one = _by_id(_gates(root, base_sha, {**env, "GRADLEW_EXIT": "1", "GRADLEW_OUTPUT": str(first)}, "detekt", run="a"))
            two = _by_id(_gates(root, base_sha, {**env, "GRADLEW_EXIT": "1", "GRADLEW_OUTPUT": str(second)}, "detekt", run="b"))

            self.assertEqual(one["detekt"]["fingerprint"], two["detekt"]["fingerprint"])

    def test_pass_has_no_excerpt(self):
        with _project() as (root, base_sha, env):

            detekt = _by_id(_gates(root, base_sha, env, "detekt"))["detekt"]

            self.assertEqual(("pass", 0, ""), (detekt["status"], detekt["exitCode"], detekt["excerpt"]))

    def test_wait_reports_running_gate(self):
        with _project() as (root, base_sha, env):
            slow = {**env, "GRADLEW_SLEEP": "4"}
            _run(root, slow, "start", "--base", base_sha, "--slug", "demo", "--run", "r1a1", "detekt")
            copy = root / "build/feature-factory/demo/r1a1/gates.py"

            code, status = _run(root, slow, "wait", "--seconds", "1", script=copy)
            code, result = _run(root, slow, "wait", script=copy)

            self.assertEqual(("running", "detekt"), (status["status"], status["gate"]))
            self.assertEqual("done", result["status"])


class OrderTests(unittest.TestCase):

    def test_later_gates_are_skipped_when_integrity_fails(self):
        with _project() as (root, base_sha, env):

            gates = _by_id(_gates(root, base_sha, env, "detekt", "integrity"))

            self.assertEqual(["integrity", "detekt"], list(gates))
            self.assertEqual(("skipped", None), (gates["detekt"]["status"], gates["detekt"]["exitCode"]))

    def test_coverage_and_e2e_are_skipped_when_unit_fails(self):
        with _project() as (root, base_sha, env):
            failing = {**env, "GRADLEW_EXIT": "1", "ADB_DEVICE": "emulator-5554"}

            gates = _by_id(_gates(root, base_sha, failing, "unit", "coverage", "e2e-desktop", "e2e-android"))

            self.assertEqual(
                ["fail", "skipped", "skipped", "skipped"],
                [gates["unit"]["status"], gates["coverage"]["status"], gates["e2e-desktop"]["status"], gates["e2e-android"]["status"]],
            )

    def test_unknown_gate_is_rejected(self):
        with _project() as (root, base_sha, env):

            code, result = _run(root, env, "start", "--base", base_sha, "--slug", "demo", "--run", "r1a1", "lint")

            self.assertEqual((2, "error"), (code, result["status"]))


class E2eGateTests(unittest.TestCase):

    def test_android_gate_runs_on_the_managed_emulator_and_stops_it(self):
        with _project() as (root, base_sha, env):

            gate = _by_id(_gates(root, base_sha, {**env, "ADB_DEVICE": "emulator-5584"}, "e2e-android"))["e2e-android"]

            self.assertIn("serial=emulator-5584", (root / gate["log"]).read_text(encoding="utf-8"))
            self.assertEqual(["start", "stop"], (Path(env["FAKE_STATE"]) / "emulator.log").read_text(encoding="utf-8").split())

    def test_android_failure_is_retried_once_on_the_running_emulator(self):
        with _project() as (root, base_sha, env):
            retry = {**env, "ADB_DEVICE": "emulator-5584", "E2E_FAIL_ONCE": str(root.parent / "failed-once")}

            gate = _by_id(_gates(root, base_sha, retry, "e2e-android"))["e2e-android"]

            log = (root / gate["log"]).read_text(encoding="utf-8")
            self.assertEqual("pass", gate["status"], log)
            self.assertIn("retrying once", log)
            self.assertEqual(["start", "stop"], (Path(env["FAKE_STATE"]) / "emulator.log").read_text(encoding="utf-8").split())

    def test_android_retry_restarts_an_emulator_that_went_offline(self):
        with _project() as (root, base_sha, env):
            retry = {**env, "ADB_DEVICE": "emulator-5584", "ADB_STATE": "offline", "E2E_FAIL_ONCE": str(root.parent / "failed-once")}

            _gates(root, base_sha, retry, "e2e-android")

            self.assertEqual(["start", "stop", "start", "stop"], (Path(env["FAKE_STATE"]) / "emulator.log").read_text(encoding="utf-8").split())

    def test_e2e_scripts_run_under_the_held_device_lock(self):
        with _project() as (root, base_sha, env):

            gate = _by_id(_gates(root, base_sha, {**env, "ADB_DEVICE": "emulator-5554"}, "e2e-android"))["e2e-android"]

            self.assertIn("lock=1", (root / gate["log"]).read_text(encoding="utf-8"))
            self.assertFalse((root / ".git/e2e.lock").exists())

    def test_ios_failure_is_reported_after_the_retry(self):
        with _project() as (root, base_sha, env):

            gate = _by_id(_gates(root, base_sha, {**env, "E2E_EXIT": "1"}, "e2e-ios"))["e2e-ios"]

            self.assertEqual(("fail", 1), (gate["status"], gate["exitCode"]))
            self.assertIn("AssertionError: expected entry", gate["excerpt"])

    def test_ios_gate_stops_android_emulators_first(self):
        with _project() as (root, base_sha, env):

            _gates(root, base_sha, {**env, "ADB_DEVICE": "emulator-5554"}, "e2e-ios")

            self.assertTrue((Path(env["FAKE_STATE"]) / "killed").exists())

    def test_desktop_gate_runs_without_the_device_lock_and_devices(self):
        with _project() as (root, base_sha, env):

            gate = _by_id(_gates(root, base_sha, {**env, "ADB_DEVICE": "emulator-5554"}, "e2e-desktop"))["e2e-desktop"]

            self.assertEqual("pass", gate["status"])
            self.assertIn("desktop-e2e.sh lock=none serial=none", (root / gate["log"]).read_text(encoding="utf-8"))
            self.assertFalse((Path(env["FAKE_STATE"]) / "emulator.log").exists())
            self.assertFalse((Path(env["FAKE_STATE"]) / "killed").exists())

    def test_desktop_failure_is_reported_without_a_retry(self):
        with _project() as (root, base_sha, env):
            failing = {**env, "E2E_FAIL_ONCE": str(root.parent / "failed-once")}

            gate = _by_id(_gates(root, base_sha, failing, "e2e-desktop"))["e2e-desktop"]

            self.assertEqual(("fail", 1), (gate["status"], gate["exitCode"]))
            self.assertNotIn("retrying once", (root / gate["log"]).read_text(encoding="utf-8"))

    def test_emulator_that_does_not_start_is_an_environment_error(self):
        with _project() as (root, base_sha, env):

            gate = _by_id(_gates(root, base_sha, {**env, "EMULATOR_START_FAILS": "1"}, "e2e-android"))["e2e-android"]

            self.assertEqual("env-error", gate["status"])
            self.assertIn("the e2e emulator did not start: The system image is missing", gate["excerpt"])

    def test_emulator_that_does_not_start_is_stopped(self):
        with _project() as (root, base_sha, env):

            _gates(root, base_sha, {**env, "EMULATOR_START_FAILS": "1"}, "e2e-android")

            self.assertEqual(["start", "stop"], (Path(env["FAKE_STATE"]) / "emulator.log").read_text(encoding="utf-8").split())


class PreflightTests(unittest.TestCase):

    def test_ready_environment_passes(self):
        with _project() as (root, base_sha, env):

            code, result = _run(root, env, "preflight", "--demo", "android,ios", "detekt", "e2e-desktop", "e2e-android", "e2e-ios")

            self.assertEqual((0, "ok"), (code, result["status"]))

    def test_missing_tools_are_reported(self):
        with _project(tools=()) as (root, base_sha, env):

            code, result = _run(root, env, "preflight", "--demo", "ios", "e2e-desktop", "e2e-android", "e2e-ios")

            self.assertEqual(3, code)
            self.assertEqual(
                {
                    "e2e-desktop": ["allure is missing"],
                    "e2e-android": ["allure is missing"],
                    "e2e-ios": ["xcodebuild is missing", "allure is missing"],
                    "demo-ios": ["xcodebuild is missing", "ffmpeg is missing"],
                },
                result["problems"],
            )

    def test_desktop_gate_needs_gradlew(self):
        with _project() as (root, base_sha, env):
            (root / "gradlew").unlink()

            code, result = _run(root, env, "preflight", "e2e-desktop")

            self.assertEqual(3, code)
            self.assertEqual({"e2e-desktop": ["./gradlew is missing or not executable"]}, result["problems"])

    def test_missing_emulator_is_reported(self):
        with _project() as (root, base_sha, env):
            (root.parent / "sdk/emulator/emulator").unlink()

            code, result = _run(root, env, "preflight", "e2e-android")

            self.assertEqual(3, code)
            self.assertEqual({"e2e-android": ["the Android emulator is missing: install it with android sdk install emulator"]}, result["problems"])


if __name__ == "__main__":
    unittest.main()
