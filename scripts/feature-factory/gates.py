#!/usr/bin/env python3
from __future__ import annotations

import argparse
import fnmatch
import hashlib
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path


ORDER = ("integrity", "detekt", "unit", "coverage", "e2e-desktop", "e2e-android", "e2e-ios")
BASIC = ("integrity", "detekt", "unit", "coverage")
GRADLE_TASKS = {"detekt": "detekt", "unit": "allTests", "coverage": "koverVerify"}
E2E_SCRIPTS = {
    "e2e-desktop": "scripts/desktop-e2e.sh",
    "e2e-android": "scripts/android-e2e-test.sh",
    "e2e-ios": "scripts/ios-e2e.sh",
}
E2E_PLATFORMS = {"e2e-android": "android", "e2e-ios": "ios"}
EMULATOR_SCRIPT = "scripts/android-emulator.sh"
PLATFORMS = ("android", "ios")
PROTECTED = (
    ".claude/**",
    ".gitignore",
    "AGENTS.md",
    "CLAUDE.md",
    "gradle.properties",
    "build-logic/src/main/kotlin/code-coverage-aggregation.gradle.kts",
    "build-logic/src/main/kotlin/code-coverage.gradle.kts",
    "build-logic/src/main/kotlin/static-analysis.gradle.kts",
    "config/detekt/**",
    "config/git-hooks/**",
    "docs/decisions/**",
    "scripts/**",
)
SPEC_FILES = ("business-spec.md", "tech-design.md")
CODE_SUFFIXES = (".kt", ".kts", ".java", ".swift")
FORBIDDEN = re.compile(r"@(?:\w+:)?Suppress(?:Warnings|Lint)?\b|@(?:[\w.]+\.)?(?:Ignore|Disabled)\b|\bXCTSkip\b")
TEST_PATH = re.compile(r"(?:^|/)src/\w*(?:Test|test)/|UITests/")
BASELINE = re.compile(r"baseline[^/]*\.xml$", re.IGNORECASE)
SOURCE_LOCATION = re.compile(r"\.kts?:\d+")
E2E_PROBLEM = re.compile(r"FAILED|[Ff]ailed|[Ee]rror:|Exception|Assertion")
SLUG = re.compile(r"[a-z0-9][a-z0-9-]*")
RUN = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*")
ANSI = re.compile(r"\x1b\[[0-9;]*[A-Za-z]")
NOISE = re.compile(r"\b\d+(?:\.\d+)?\s?(?:ms|s|m|h)\b|\b\d{1,2}:\d{2}:\d{2}(?:\.\d+)?\b|0x[0-9a-fA-F]+")
IOS_DEVICE = os.environ.get("DEVICE", "iPhone 17 Pro")
IOS_VERSION = os.environ.get("OS_VERSION", "26.5")
GATE_TIMEOUTS = {"detekt": 1800, "unit": 3600, "coverage": 3600, "e2e-desktop": 1800, "e2e-android": 2700, "e2e-ios": 2700}
EMULATOR_START_TIMEOUT = 1800
EMULATOR_STOP_TIMEOUT = 300
WAIT_SECONDS = 540
MAX_EXCERPT_LINES = 80
MAX_EXCERPT_CHARS = 8000


class Error(Exception):
    pass


class EnvError(Exception):
    pass


def git(root, *arguments):
    completed = subprocess.run(["git", "-C", str(root), *arguments], capture_output=True, text=True)
    if completed.returncode != 0:
        raise Error(f"git {' '.join(arguments)}: {completed.stderr.strip()}")
    return completed.stdout


def repo_root():
    return Path(git(Path.cwd(), "rev-parse", "--show-toplevel").strip())


def write_json(path, value):
    temporary = path.with_name(f".{path.name}.tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")
    os.replace(temporary, path)


def ordered(gates):
    unknown = sorted(set(gates) - set(ORDER))
    if unknown:
        raise Error(f"unknown gates: {', '.join(unknown)}")
    return [gate for gate in ORDER if gate in gates]


def matches(path, pattern):
    target = path if "/" in pattern else path.rsplit("/", 1)[-1]
    return fnmatch.fnmatchcase(target, pattern)


def allowed(path, patterns):
    return any(matches(path, pattern) for pattern in patterns)


def added_lines(root, base):
    path = None
    in_hunk = False
    diff = git(root, "-c", "core.quotePath=false", "diff", "-U0", "--no-color", "--no-ext-diff", f"{base}..HEAD")
    for line in diff.splitlines():
        if line.startswith("diff --git "):
            path = None
            in_hunk = False
        elif not in_hunk and line.startswith("+++ "):
            path = line[6:] if line.startswith("+++ b/") else None
        elif line.startswith("@@"):
            in_hunk = True
        elif in_hunk and line.startswith("+") and path is not None:
            yield path, line[1:]


def integrity(root, base, slug, allow):
    problems = []
    status = git(root, "status", "--porcelain", "--untracked-files=all").rstrip()
    if status:
        problems.append("uncommitted changes:\n" + status)
    if git(root, "rev-parse", "HEAD").strip() == base:
        problems.append("no commits after the base")
    protected = PROTECTED + tuple(f"docs/features/{slug}/{name}" for name in SPEC_FILES)
    changes = git(root, "-c", "core.quotePath=false", "diff", "--name-status", "-M", f"{base}..HEAD")
    for line in changes.splitlines():
        kind, *paths = line.split("\t")
        for path in paths:
            if allowed(path, protected) and not allowed(path, allow):
                problems.append(f"protected path changed: {path}")
        old, new = paths[0], paths[-1]
        if kind[0] in "DR" and TEST_PATH.search(old) and not allowed(old, allow):
            problems.append(f"test file {'deleted' if kind[0] == 'D' else 'renamed'}: {old}")
        if kind[0] in "ACR" and BASELINE.search(new):
            problems.append(f"baseline file added: {new}")
    for path, line in added_lines(root, base):
        if path.endswith(CODE_SUFFIXES) and FORBIDDEN.search(line):
            problems.append(f"forbidden line added in {path}: {line.strip()}")
    return list(dict.fromkeys(problems))


def bounded(lines):
    unique = []
    for line in lines:
        if not unique or unique[-1] != line:
            unique.append(line)
    return "\n".join(unique[:MAX_EXCERPT_LINES])[:MAX_EXCERPT_CHARS]


def fingerprint(lines, root):
    prefix = f"{root}/"
    normalized = [NOISE.sub("#", ANSI.sub("", line)).replace(prefix, "").strip() for line in lines]
    text = "\n".join(line for line in normalized if line)
    return hashlib.sha256(text.encode()).hexdigest()[:16]


def gradle_lines(text):
    picked = []
    inside = False
    lines = ANSI.sub("", text).splitlines()
    for line in lines:
        if line.startswith("* What went wrong:"):
            inside = True
        elif line.startswith(("* Try:", "* Exception is:", "* Get more help")):
            inside = False
        if line.startswith("w: "):
            continue
        if inside or line.startswith("e: ") or line.endswith(" FAILED") or SOURCE_LOCATION.search(line):
            picked.append(line)
    return picked or lines[-40:]


def e2e_lines(text):
    lines = ANSI.sub("", text).splitlines()
    picked = [line for line in lines if E2E_PROBLEM.search(line)]
    return picked[-50:] or lines[-40:]


def sdk_dir(root):
    properties = root / "local.properties"
    if properties.exists():
        for line in properties.read_text(encoding="utf-8").splitlines():
            if line.startswith("sdk.dir="):
                return Path(line[len("sdk.dir="):].strip())
    return None


def tool_env(root):
    env = dict(os.environ)
    sdk = sdk_dir(root)
    if sdk is not None:
        env["PATH"] = os.pathsep.join([str(sdk / "platform-tools"), str(sdk / "emulator"), env.get("PATH", "")])
    return env


def quiet(command, env, timeout=120):
    try:
        return subprocess.run(command, capture_output=True, text=True, env=env, timeout=timeout)
    except (OSError, subprocess.TimeoutExpired):
        return None


def adb_serials(env):
    completed = quiet(["adb", "devices"], env)
    if completed is None or completed.returncode != 0:
        return {}
    serials = {}
    for line in completed.stdout.splitlines()[1:]:
        parts = line.split("\t")
        if len(parts) == 2:
            serials[parts[0]] = parts[1]
    return serials


def simulator_udid(env):
    completed = quiet(["xcrun", "simctl", "list", "devices", "available"], env)
    if completed is None or completed.returncode != 0:
        return None
    inside = False
    for line in completed.stdout.splitlines():
        if line.startswith("-- "):
            inside = line == f"-- iOS {IOS_VERSION} --"
        elif inside and line.startswith(f"    {IOS_DEVICE} ("):
            found = re.search(r"\(([0-9A-F-]{36})\)", line)
            if found:
                return found.group(1)
    return None


def platform_problems(root, platform, env):
    problems = []
    if platform == "android":
        sdk = sdk_dir(root)
        if sdk is None or not (sdk / "platform-tools" / "adb").exists():
            problems.append("adb is missing: set sdk.dir in local.properties to the Android SDK")
        elif not (sdk / "emulator" / "emulator").exists():
            problems.append("the Android emulator is missing: install it with android sdk install emulator")
    else:
        if shutil.which("xcodebuild", path=env.get("PATH")) is None:
            problems.append("xcodebuild is missing")
        elif simulator_udid(env) is None:
            problems.append(f"no simulator \"{IOS_DEVICE}\" on iOS {IOS_VERSION}: install the runtime or set DEVICE and OS_VERSION")
    return problems


def preflight(root, gates, demo, env):
    problems = {}
    for gate in gates:
        found = []
        if gate in GRADLE_TASKS or (gate in E2E_SCRIPTS and gate not in E2E_PLATFORMS):
            if not os.access(root / "gradlew", os.X_OK):
                found.append("./gradlew is missing or not executable")
            sdk = sdk_dir(root)
            if sdk is None or not sdk.is_dir():
                found.append("sdk.dir in local.properties does not point to an Android SDK")
        if gate in E2E_PLATFORMS:
            found.extend(platform_problems(root, E2E_PLATFORMS[gate], env))
        if gate in E2E_SCRIPTS and shutil.which("allure", path=env.get("PATH")) is None:
            found.append("allure is missing")
        if found:
            problems[gate] = found
    for platform in demo:
        found = platform_problems(root, platform, env)
        if shutil.which("ffmpeg", path=env.get("PATH")) is None:
            found.append("ffmpeg is missing")
        if found:
            problems[f"demo-{platform}"] = found
    return problems


def alive(pid):
    try:
        os.kill(pid, 0)
    except ProcessLookupError:
        return False
    except PermissionError:
        return True
    return True


def lock_path(root):
    return Path(git(root, "rev-parse", "--path-format=absolute", "--git-common-dir").strip()) / "e2e.lock"


def acquire_lock(root, log):
    path = lock_path(root)
    while True:
        try:
            path.mkdir()
        except FileExistsError:
            try:
                holder = int((path / "pid").read_text().strip())
            except (OSError, ValueError):
                holder = None
            if holder is not None and not alive(holder):
                shutil.rmtree(path, ignore_errors=True)
                continue
            log(f"waiting for the e2e device lock held by process {holder or 'unknown'}: {path}")
            time.sleep(10)
            continue
        (path / "pid").write_text(str(os.getpid()))
        return path


def release_lock(path):
    try:
        if (path / "pid").read_text().strip() == str(os.getpid()):
            shutil.rmtree(path, ignore_errors=True)
    except OSError:
        pass


def shutdown_simulators(env):
    if shutil.which("xcrun", path=env.get("PATH")) is not None:
        quiet(["xcrun", "simctl", "shutdown", "all"], env)


def kill_emulators(env):
    emulators = [serial for serial in adb_serials(env) if serial.startswith("emulator-")]
    for serial in emulators:
        quiet(["adb", "-s", serial, "emu", "kill"], env)
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline and any(serial in adb_serials(env) for serial in emulators):
        time.sleep(2)


def stop_group(process):
    try:
        os.killpg(process.pid, signal.SIGTERM)
        process.wait(timeout=30)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGKILL)
        process.wait()
    except ProcessLookupError:
        pass


def android_up(script, env, log):
    shutdown_simulators(env)
    completed = quiet([str(script), "start"], env, EMULATOR_START_TIMEOUT)
    if completed is None:
        raise EnvError(f"the e2e emulator did not start in {EMULATOR_START_TIMEOUT} s")
    if completed.stderr.strip():
        log(completed.stderr.rstrip())
    serials = completed.stdout.split()
    if completed.returncode != 0 or not serials:
        reasons = completed.stderr.strip().splitlines() or ["no serial"]
        raise EnvError(f"the e2e emulator did not start: {reasons[-1]}")
    return serials[-1]


def android_down(script, env):
    quiet([str(script), "stop"], env, EMULATOR_STOP_TIMEOUT)


def run_command(command, root, log, timeout, env):
    with open(log, "a", encoding="utf-8", errors="replace") as handle:
        handle.write(f"$ {' '.join(command)}\n")
        handle.flush()
        process = subprocess.Popen(
            command,
            cwd=root,
            stdin=subprocess.DEVNULL,
            stdout=handle,
            stderr=subprocess.STDOUT,
            env=env,
            start_new_session=True,
        )
        try:
            return process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            stop_group(process)
            handle.write(f"\ntimed out after {timeout} s\n")
            return None


def outcome(gate, status, exit_code, lines, log, started, root):
    return {
        "id": gate,
        "status": status,
        "exitCode": exit_code,
        "durationSec": round(time.monotonic() - started),
        "log": str(log.relative_to(root)) if log is not None else None,
        "fingerprint": fingerprint(lines, root) if status == "fail" else "",
        "excerpt": bounded(lines) if status != "pass" else "",
    }


def run_e2e(gate, root, log, env, note):
    command = [str(root / E2E_SCRIPTS[gate])]
    if gate not in E2E_PLATFORMS:
        return run_command(command, root, log, GATE_TIMEOUTS[gate], env)
    emulator = root / EMULATOR_SCRIPT
    child = dict(env, E2E_DEVICE_LOCK_HELD="1")
    lock = acquire_lock(root, note)
    try:
        if gate == "e2e-android":
            child["ANDROID_SERIAL"] = android_up(emulator, env, note)
        else:
            kill_emulators(env)
        code = run_command(command, root, log, GATE_TIMEOUTS[gate], child)
        if code not in (0, None):
            with open(log, "a", encoding="utf-8") as handle:
                handle.write("\nretrying once\n")
            if gate == "e2e-android" and adb_serials(env).get(child["ANDROID_SERIAL"]) != "device":
                android_down(emulator, env)
                child["ANDROID_SERIAL"] = android_up(emulator, env, note)
            code = run_command(command, root, log, GATE_TIMEOUTS[gate], child)
        return code
    finally:
        if gate == "e2e-android":
            android_down(emulator, env)
        else:
            shutdown_simulators(env)
        release_lock(lock)


def run_gate(gate, request, run_dir, env):
    root = Path(request["root"])
    log = run_dir / f"{gate}.log"
    started = time.monotonic()

    def note(message):
        with open(log, "a", encoding="utf-8") as handle:
            handle.write(f"{message}\n")

    if gate == "integrity":
        problems = integrity(root, request["base"], request["slug"], request["allow"])
        log.write_text("".join(f"{problem}\n" for problem in problems), encoding="utf-8")
        lines = [line for problem in problems for line in problem.splitlines()]
        return outcome(gate, "fail" if problems else "pass", 1 if problems else 0, lines, log, started, root)
    if gate in GRADLE_TASKS:
        command = ["./gradlew", "--console=plain", GRADLE_TASKS[gate]]
        code = run_command(command, root, log, GATE_TIMEOUTS[gate], env)
        text = log.read_text(encoding="utf-8", errors="replace")
        if code == 0:
            return outcome(gate, "pass", 0, [], log, started, root)
        return outcome(gate, "fail", 124 if code is None else code, gradle_lines(text), log, started, root)
    try:
        code = run_e2e(gate, root, log, env, note)
    except EnvError as error:
        note(str(error))
        return outcome(gate, "env-error", None, [str(error)], log, started, root)
    text = log.read_text(encoding="utf-8", errors="replace")
    if code == 0:
        return outcome(gate, "pass", 0, [], log, started, root)
    if gate == "e2e-ios" and "No simulator \"" in text:
        return outcome(gate, "env-error", None, e2e_lines(text), log, started, root)
    return outcome(gate, "fail", 124 if code is None else code, e2e_lines(text), log, started, root)


def skip_reason(gate, results):
    def failed(other):
        return other in results and results[other]["status"] != "pass"

    if gate != "integrity" and failed("integrity"):
        return "skipped: integrity failed"
    if gate == "coverage" and failed("unit"):
        return "skipped: unit failed"
    if gate in E2E_SCRIPTS and any(failed(other) for other in BASIC):
        return "skipped: an earlier gate failed"
    return None


def skipped(gate, reason):
    return {"id": gate, "status": "skipped", "exitCode": None, "durationSec": 0, "log": None, "fingerprint": "", "excerpt": reason}


def work(run_dir):
    request = json.loads((run_dir / "request.json").read_text(encoding="utf-8"))
    root = Path(request["root"])
    env = tool_env(root)
    results = {}
    problems = preflight(root, request["gates"], [], env)
    for gate in request["gates"]:
        write_json(run_dir / "status.json", {"status": "running", "run": request["run"], "gate": gate, "done": list(results)})
        if gate in problems:
            results[gate] = {**skipped(gate, "\n".join(problems[gate])), "status": "env-error"}
        elif problems:
            results[gate] = skipped(gate, "skipped: the environment is not ready")
        elif skip_reason(gate, results):
            results[gate] = skipped(gate, skip_reason(gate, results))
        else:
            results[gate] = run_gate(gate, request, run_dir, env)
    write_json(
        run_dir / "result.json",
        {"status": "done", "run": request["run"], "base": request["base"], "head": request["head"], "gates": list(results.values())},
    )


def start(arguments):
    root = repo_root()
    if not SLUG.fullmatch(arguments.slug):
        raise Error(f"invalid slug: {arguments.slug}")
    if not RUN.fullmatch(arguments.run):
        raise Error(f"invalid run label: {arguments.run}")
    gates = ordered(arguments.gates)
    base = git(root, "rev-parse", "--verify", f"{arguments.base}^{{commit}}").strip()
    run_dir = root / "build" / "feature-factory" / arguments.slug / arguments.run
    if run_dir.exists():
        shutil.rmtree(run_dir)
    run_dir.mkdir(parents=True)
    (run_dir / "gates.py").write_text(git(root, "show", f"{base}:scripts/feature-factory/gates.py"), encoding="utf-8")
    request = {
        "root": str(root),
        "base": base,
        "head": git(root, "rev-parse", "HEAD").strip(),
        "slug": arguments.slug,
        "run": arguments.run,
        "gates": gates,
        "allow": arguments.allow,
    }
    write_json(run_dir / "request.json", request)
    write_json(run_dir / "status.json", {"status": "running", "run": arguments.run, "gate": None, "done": []})
    with open(run_dir / "runner.log", "w", encoding="utf-8") as handle:
        process = subprocess.Popen(
            [sys.executable, str(run_dir / "gates.py"), "run", str(run_dir)],
            cwd=root,
            stdin=subprocess.DEVNULL,
            stdout=handle,
            stderr=subprocess.STDOUT,
            start_new_session=True,
        )
    (run_dir / "worker.pid").write_text(str(process.pid), encoding="utf-8")
    relative = run_dir.relative_to(root)
    print(json.dumps({"status": "started", "run": arguments.run, "dir": str(relative), "wait": f"python3 {relative}/gates.py wait"}))
    return 0


def wait(arguments):
    run_dir = Path(__file__).resolve().parent
    if not (run_dir / "request.json").exists():
        raise Error("wait runs only from the copy in a run directory, as printed by start")
    run = json.loads((run_dir / "request.json").read_text(encoding="utf-8"))["run"]
    deadline = time.monotonic() + arguments.seconds
    while True:
        result = run_dir / "result.json"
        if result.exists():
            print(json.dumps(json.loads(result.read_text(encoding="utf-8"))))
            return 0
        pid = int((run_dir / "worker.pid").read_text(encoding="utf-8"))
        if not alive(pid) and not result.exists():
            log = (run_dir / "runner.log").read_text(encoding="utf-8", errors="replace").splitlines()[-20:]
            print(json.dumps({"status": "error", "run": run, "message": "the gate runner stopped without a result", "log": "\n".join(log)}))
            return 0
        if time.monotonic() >= deadline:
            print(json.dumps(json.loads((run_dir / "status.json").read_text(encoding="utf-8"))))
            return 0
        time.sleep(2)


def preflight_command(arguments):
    root = repo_root()
    demo = [platform for platform in arguments.demo.split(",") if platform]
    unknown = sorted(set(demo) - set(PLATFORMS))
    if unknown:
        raise Error(f"unknown demo platforms: {', '.join(unknown)}")
    problems = preflight(root, ordered(arguments.gates), demo, tool_env(root))
    print(json.dumps({"status": "env-error" if problems else "ok", "problems": problems}))
    return 3 if problems else 0


def run_command_line(arguments):
    run_dir = Path(arguments.dir)
    try:
        work(run_dir)
    except Exception as error:
        write_json(run_dir / "result.json", {"status": "error", "run": run_dir.name, "message": f"{type(error).__name__}: {error}"})
        raise
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description="Run the mandatory verification gates of a feature build.")
    commands = parser.add_subparsers(dest="command", required=True)
    check = commands.add_parser("preflight")
    check.add_argument("--demo", default="")
    check.add_argument("gates", nargs="*")
    begin = commands.add_parser("start")
    begin.add_argument("--base", required=True)
    begin.add_argument("--slug", required=True)
    begin.add_argument("--run", required=True)
    begin.add_argument("--allow", action="append", default=[])
    begin.add_argument("gates", nargs="+")
    hold = commands.add_parser("wait")
    hold.add_argument("--seconds", type=int, default=WAIT_SECONDS)
    worker = commands.add_parser("run")
    worker.add_argument("dir")
    arguments = parser.parse_args(argv)
    handlers = {"preflight": preflight_command, "start": start, "wait": wait, "run": run_command_line}
    try:
        return handlers[arguments.command](arguments)
    except Error as error:
        print(json.dumps({"status": "error", "message": str(error)}))
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
