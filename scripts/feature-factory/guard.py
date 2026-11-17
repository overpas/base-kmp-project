#!/usr/bin/env python3
from __future__ import annotations

import fnmatch
import json
import re
import subprocess
import sys
from pathlib import Path


CODER_PROTECTED = (
    ".claude/**",
    "scripts/feature-factory/**",
    "docs/decisions/**",
    "docs/features/*/business-spec.md",
    "docs/features/*/tech-design.md",
)
EDIT_TOOLS = {"Edit": "file_path", "Write": "file_path", "MultiEdit": "file_path", "NotebookEdit": "notebook_path"}
GIT = r"\bgit(?:\s+-[Cc]\s+\S+|\s+--[\w-]+(?:=\S+)?)*\s+"
CODER_COMMANDS = (
    (re.compile(r"--no-verify\b"), "commits must run the pre-commit hook: drop --no-verify"),
    (re.compile(GIT + r"commit\b[^|;&\n]*\s-n\b"), "commits must run the pre-commit hook: drop -n"),
    (re.compile(r"core\.hooksPath|" + GIT + r"config\b"), "the git configuration is not part of the task"),
    (re.compile(GIT + r"(?:push|switch|rebase|merge|stash)\b"), "stay on the feature branch: no push, switch, rebase, merge or stash"),
    (re.compile(GIT + r"reset\b[^|;&\n]*--hard\b"), "keep the committed work: no git reset --hard"),
    (re.compile(GIT + r"checkout\b(?![^|;&\n]*\s--(?:\s|$))"), "stay on the feature branch: use git checkout only with -- <paths>"),
)
DEMO_COMMANDS = ((re.compile(GIT + r"commit\b"), "the demo changes no tracked file and makes no commit"),) + CODER_COMMANDS
DEMO_OUTPUT = "build/feature-factory/"
VERIFIER_COMMANDS = (
    re.compile(r"git show [0-9a-f]{7,40}:scripts/feature-factory/gates\.py \| python3 - start [A-Za-z0-9 ._*/:='-]+"),
    re.compile(r"python3 build/feature-factory/[a-z0-9][a-z0-9-]*/[A-Za-z0-9][A-Za-z0-9._-]*/gates\.py wait(?: --seconds \d+)?"),
)


def decision(kind, reason):
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": kind, "permissionDecisionReason": reason}}))
    return 0


def worktree_root(cwd):
    completed = subprocess.run(["git", "-C", cwd, "rev-parse", "--show-toplevel"], capture_output=True, text=True)
    if completed.returncode != 0:
        return None
    return Path(completed.stdout.strip()).resolve()


def edit_target(event):
    tool = event.get("tool_name")
    if tool not in EDIT_TOOLS:
        return None, None
    root = worktree_root(event.get("cwd") or ".")
    target = (event.get("tool_input") or {}).get(EDIT_TOOLS[tool])
    if root is None or not target:
        return None, "the guard cannot resolve the worktree or the target path"
    path = (Path(event.get("cwd") or ".") / target).resolve()
    if root != path and root not in path.parents:
        return None, f"write only inside the worktree {root}"
    return path.relative_to(root).as_posix(), None


def denied_command(event, rules):
    if event.get("tool_name") != "Bash":
        return None
    command = (event.get("tool_input") or {}).get("command") or ""
    return next((reason for pattern, reason in rules if pattern.search(command)), None)


def coder(event):
    relative, problem = edit_target(event)
    if problem:
        return decision("deny", problem)
    if relative is not None and any(fnmatch.fnmatchcase(relative, pattern) for pattern in CODER_PROTECTED):
        return decision("deny", f"{relative} is a protected file of the feature factory; report a blocker if the plan needs it changed")
    reason = denied_command(event, CODER_COMMANDS)
    return decision("deny", reason) if reason else 0


def demo(event):
    relative, problem = edit_target(event)
    if problem:
        return decision("deny", problem)
    if relative is not None and not relative.startswith(DEMO_OUTPUT):
        return decision("deny", f"the demo writes only under {DEMO_OUTPUT}")
    reason = denied_command(event, DEMO_COMMANDS)
    return decision("deny", reason) if reason else 0


def verifier(event):
    command = (event.get("tool_input") or {}).get("command") or ""
    if event.get("tool_name") == "Bash" and any(pattern.fullmatch(command.strip()) for pattern in VERIFIER_COMMANDS):
        return decision("allow", "a feature factory gate runner command")
    return decision("deny", "the verifier runs only the gate runner commands that its prompt gives")


def main(argv=None):
    arguments = sys.argv[1:] if argv is None else argv
    modes = {"coder": coder, "demo": demo, "verifier": verifier}
    if len(arguments) != 1 or arguments[0] not in modes:
        return decision("deny", "the guard needs one mode: coder, demo or verifier")
    try:
        event = json.loads(sys.stdin.read())
    except ValueError:
        return decision("deny", "the guard cannot read the hook input")
    if not isinstance(event, dict):
        return decision("deny", "the guard cannot read the hook input")
    return modes[arguments[0]](event)


if __name__ == "__main__":
    raise SystemExit(main())
