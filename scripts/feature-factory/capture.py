#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import threading
import time
from pathlib import Path

import gates


LABEL = re.compile(r"[a-z0-9][a-z0-9-]*")
IOS_STARTED = re.compile(r"Test Case '.+' started")
IOS_FINISHED = re.compile(r"Test Case '.+' (?:passed|failed)")
STILLS = 8
XCRESULT_SUFFIX = re.compile(r"(?:_\d+_[0-9A-Fa-f-]{36})?\.(?:png|jpe?g)$", re.IGNORECASE)
IMAGE_TYPES = {"image/png", "image/jpeg"}
IMAGE_SUFFIXES = {".png", ".jpeg", ".jpg"}
TAIL_SECONDS = 0.8
ANDROID_STEP_SCREENSHOTS = "-Pandroid.testInstrumentationRunnerArguments.stepScreenshots=true"
IOS_STEP_SCREENSHOTS = {"TEST_RUNNER_STEP_SCREENSHOTS": "1"}
EMULATOR_SCRIPT = Path(__file__).resolve().parents[1] / "android-emulator.sh"


class Recording:

    def __init__(self):
        self.process = None
        self.started = None
        self.finished = None
        self.lock = threading.Lock()

    def begin(self, command, env):
        with self.lock:
            if self.process is None:
                self.process = subprocess.Popen(command, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, env=env)
                self.started = time.monotonic()

    def end(self):
        with self.lock:
            if self.process is not None:
                self.finished = time.monotonic()

    def duration(self):
        if self.started is None or self.finished is None:
            return None
        return self.finished - self.started + TAIL_SECONDS


def ffmpeg(*arguments):
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", *arguments], check=True)


def finish_media(raw, out, label, duration):
    video = out / f"{label}.mp4"
    trim = ["-t", f"{duration:.2f}"] if duration else []
    ffmpeg("-i", str(raw), *trim, "-an", "-vf", "scale='min(720,iw)':-2", "-c:v", "libx264", "-preset", "veryfast", "-crf", "28", "-pix_fmt", "yuv420p", "-movflags", "+faststart", str(video))
    ffmpeg("-i", str(video), "-vf", "select='eq(n,0)+gt(scene,0.25)'", "-fps_mode", "vfr", "-frames:v", str(STILLS), str(out / f"{label}-%02d.png"))
    ffmpeg("-sseof", "-0.3", "-i", str(video), "-frames:v", "1", "-update", "1", str(out / f"{label}-last.png"))
    raw.unlink()
    return video, sorted(out.glob(f"{label}-*.png"))


def allure_step_shots(results, out, label):
    shots = []

    def walk(node):
        for child in node.get("steps", []):
            for attachment in child.get("attachments", []):
                if attachment.get("type") in IMAGE_TYPES:
                    shots.append((results / attachment["source"], child["name"]))
            walk(child)

    for result in sorted(results.glob("*-result.json")):
        walk(read_result(result))
    return keep_step_shots(shots, out, label)


def read_result(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except ValueError:
        return {}


def xcresult_step_shots(exported, out, label):
    manifest = exported / "manifest.json"
    if not manifest.exists():
        return []
    attachments = [
        attachment
        for test in json.loads(manifest.read_text(encoding="utf-8"))
        for attachment in test.get("attachments", [])
        if Path(attachment["exportedFileName"]).suffix.lower() in IMAGE_SUFFIXES
    ]
    attachments.sort(key=lambda attachment: attachment.get("timestamp", 0))
    shots = [(exported / attachment["exportedFileName"], step_name(attachment["suggestedHumanReadableName"])) for attachment in attachments]
    return keep_step_shots(shots, out, label)


def step_name(readable):
    return XCRESULT_SUFFIX.sub("", readable)


def keep_step_shots(shots, out, label):
    kept = []
    for index, (source, caption) in enumerate((shot for shot in shots if shot[0].exists()), start=1):
        target = out / f"{label}-step-{index:02d}{source.suffix.lower()}"
        shutil.copyfile(source, target)
        kept.append((target, caption))
    return kept


def use_step_shots(stills, shots):
    if not shots:
        return stills, {}
    for still in stills:
        still.unlink()
    return [shot for shot, _ in shots], {shot.name: caption for shot, caption in shots}


def export_xcresult(xcresult, env):
    exported = Path(tempfile.mkdtemp(prefix="ff-xcresult-"))
    subprocess.run(
        ["xcrun", "xcresulttool", "export", "attachments", "--path", str(xcresult), "--output-path", str(exported)],
        env=env,
        check=False,
        capture_output=True,
    )
    return exported


def stream(process, log, on_line):
    with open(log, "a", encoding="utf-8", errors="replace") as handle:
        for line in process.stdout:
            handle.write(line)
            on_line(line)


def log_error(message):
    print(message, file=sys.stderr)


def capture_android(root, test, out, label, env):
    remote = f"/sdcard/ff-{label}.mp4"
    recording = Recording()
    try:
        serial = gates.android_up(EMULATOR_SCRIPT, env, log_error)
        adb = ["adb", "-s", serial]
        subprocess.run([*adb, "logcat", "-c"], env=env, check=False)
        logcat = subprocess.Popen([*adb, "logcat", "-v", "brief", "TestRunner:I", "*:S"], stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, env=env)

        def on_logcat(line):
            if "started:" in line:
                recording.begin([*adb, "shell", "screenrecord", "--bit-rate", "6000000", remote], env)
            elif "finished:" in line:
                recording.end()

        reader = threading.Thread(target=stream, args=(logcat, out / f"{label}-logcat.log", on_logcat), daemon=True)
        reader.start()
        command = [str(root / "scripts/android-e2e-test.sh"), f"-Pandroid.testInstrumentationRunnerArguments.class={test}", ANDROID_STEP_SCREENSHOTS]
        code = gates.run_command(command, root, out / f"{label}-e2e.log", gates.GATE_TIMEOUTS["e2e-android"], dict(env, E2E_DEVICE_LOCK_HELD="1", ANDROID_SERIAL=serial))
        subprocess.run([*adb, "shell", "pkill", "-INT", "screenrecord"], env=env, check=False)
        time.sleep(3)
        logcat.terminate()
        if recording.process is None:
            return code, None, [], {}
        recording.process.wait(timeout=30)
        raw = out / f"{label}-raw.mp4"
        subprocess.run([*adb, "pull", remote, str(raw)], env=env, check=True, capture_output=True)
        subprocess.run([*adb, "shell", "rm", "-f", remote], env=env, check=False)
        video, stills = finish_media(raw, out, label, recording.duration())
        stills, captions = use_step_shots(stills, allure_step_shots(root / ".allure/android/results", out, label))
        return code, video, stills, captions
    finally:
        gates.android_down(EMULATOR_SCRIPT, env)


def capture_ios(root, test, out, label, env):
    gates.kill_emulators(env)
    udid = gates.simulator_udid(env)
    if udid is None:
        raise gates.EnvError(f"no simulator \"{gates.IOS_DEVICE}\" on iOS {gates.IOS_VERSION}")
    subprocess.run(["xcrun", "simctl", "boot", udid], env=env, check=False, capture_output=True)
    subprocess.run(["xcrun", "simctl", "bootstatus", udid], env=env, check=False, capture_output=True)
    raw = out / f"{label}-raw.mov"
    recording = Recording()
    try:
        process = subprocess.Popen(
            [str(root / "scripts/ios-e2e.sh"), f"-only-testing:{test}"],
            cwd=root,
            stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            env=dict(env, E2E_DEVICE_LOCK_HELD="1", **IOS_STEP_SCREENSHOTS),
            start_new_session=True,
        )

        def on_output(line):
            if IOS_STARTED.search(line):
                recording.begin(["xcrun", "simctl", "io", udid, "recordVideo", "--codec", "h264", "--force", str(raw)], env)
            elif IOS_FINISHED.search(line):
                recording.end()

        watchdog = threading.Timer(gates.GATE_TIMEOUTS["e2e-ios"], gates.stop_group, args=(process,))
        watchdog.start()
        stream(process, out / f"{label}-e2e.log", on_output)
        code = process.wait()
        watchdog.cancel()
        if recording.process is None:
            return code, None, [], {}
        recording.process.send_signal(signal.SIGINT)
        recording.process.wait(timeout=30)
        video, stills = finish_media(raw, out, label, recording.duration())
        exported = export_xcresult(root / ".allure/ios/results/e2e.xcresult", env)
        try:
            stills, captions = use_step_shots(stills, xcresult_step_shots(exported, out, label))
        finally:
            shutil.rmtree(exported, ignore_errors=True)
        return code, video, stills, captions
    finally:
        gates.shutdown_simulators(env)


def capture(arguments):
    root = Path(arguments.root).resolve() if arguments.root else gates.repo_root()
    if not LABEL.fullmatch(arguments.label):
        raise gates.Error(f"invalid label: {arguments.label}")
    out = Path(arguments.out).resolve()
    out.mkdir(parents=True, exist_ok=True)
    env = gates.tool_env(root)
    lock = gates.acquire_lock(root, print)
    try:
        if arguments.platform == "android":
            code, video, stills, captions = capture_android(root, arguments.test, out, arguments.label, env)
        else:
            code, video, stills, captions = capture_ios(root, arguments.test, out, arguments.label, env)
    except gates.EnvError as error:
        print(json.dumps({"status": "env-error", "message": str(error)}))
        return 3
    finally:
        gates.release_lock(lock)
    summary = {
        "status": "captured" if video else "no-recording",
        "platform": arguments.platform,
        "test": arguments.test,
        "testExitCode": code,
        "video": video.name if video else None,
        "stills": [still.name for still in stills],
        "captions": captions,
        "log": f"{arguments.label}-e2e.log",
    }
    (out / f"{arguments.label}.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary))
    return 0


def export(arguments):
    if not gates.SLUG.fullmatch(arguments.slug):
        raise gates.Error(f"invalid slug: {arguments.slug}")
    source = Path(arguments.dir).resolve()
    if not (source / "index.html").exists():
        raise gates.Error(f"{source} has no index.html")
    root = gates.repo_root()
    main_checkout = Path(gates.git(root, "rev-parse", "--path-format=absolute", "--git-common-dir").strip()).parent
    target = main_checkout / ".demos" / arguments.slug
    if target.exists():
        shutil.rmtree(target)
    shutil.copytree(source, target)
    print(json.dumps({"status": "exported", "path": str(target / "index.html")}))
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description="Record feature demos on a device and export them.")
    commands = parser.add_subparsers(dest="command", required=True)
    for platform in gates.PLATFORMS:
        record = commands.add_parser(platform)
        record.add_argument("--test", required=True)
        record.add_argument("--out", required=True)
        record.add_argument("--label", required=True)
        record.add_argument("--root")
        record.set_defaults(platform=platform, handler=capture)
    publish = commands.add_parser("export")
    publish.add_argument("dir")
    publish.add_argument("--slug", required=True)
    publish.set_defaults(handler=export)
    arguments = parser.parse_args(argv)
    try:
        return arguments.handler(arguments)
    except (gates.Error, subprocess.CalledProcessError) as error:
        print(json.dumps({"status": "error", "message": str(error)}))
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
