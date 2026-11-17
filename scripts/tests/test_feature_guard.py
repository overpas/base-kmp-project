import contextlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


_GUARD = Path(__file__).resolve().parents[2] / "scripts/feature-factory/guard.py"


@contextlib.contextmanager
def _worktree():
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory).resolve() / "worktree"
        root.mkdir()
        subprocess.run(["git", "init", "-q", str(root)], check=True)
        yield root


def _guard(mode, event, cwd=None):
    completed = subprocess.run(
        [sys.executable, str(_GUARD), mode],
        input=json.dumps(event) if isinstance(event, dict) else event,
        cwd=cwd,
        capture_output=True,
        text=True,
        timeout=60,
    )
    if not completed.stdout.strip():
        return None
    return json.loads(completed.stdout)["hookSpecificOutput"]["permissionDecision"]


def _bash(command, cwd="."):
    return {"tool_name": "Bash", "tool_input": {"command": command}, "cwd": str(cwd)}


def _write(path, cwd):
    return {"tool_name": "Write", "tool_input": {"file_path": str(path), "content": "x"}, "cwd": str(cwd)}


class CoderGuardTests(unittest.TestCase):

    def test_write_inside_the_worktree_is_left_to_the_permission_rules(self):
        with _worktree() as root:

            result = _guard("coder", _write(root / "feature/src/commonMain/kotlin/Feature.kt", root))

            self.assertIsNone(result)

    def test_write_to_protected_files_is_denied(self):
        with _worktree() as root:
            targets = [
                root / ".claude/agents/code-reviewer.md",
                root / "scripts/feature-factory/gates.py",
                root / "docs/decisions/0001-use-sqlcipher.md",
                root / "docs/features/search/business-spec.md",
                root / "docs/features/search/tech-design.md",
            ]

            results = [_guard("coder", _write(target, root)) for target in targets]

            self.assertEqual(["deny"] * 5, results)

    def test_write_outside_the_worktree_is_denied(self):
        with _worktree() as root:

            result = _guard("coder", _write(root.parent / "elsewhere.kt", root))

            self.assertEqual("deny", result)

    def test_relative_path_resolves_against_the_agent_directory(self):
        with _worktree() as root:

            result = _guard("coder", {"tool_name": "Edit", "tool_input": {"file_path": ".claude/settings.json"}, "cwd": str(root)})

            self.assertEqual("deny", result)

    def test_history_and_hook_bypass_commands_are_denied(self):
        commands = [
            'git commit --no-verify -m "Add search"',
            'git commit -n -m "Add search"',
            "git -C /repo push origin feature",
            "git switch develop",
            "git checkout develop",
            "git checkout -b other",
            "git reset --hard HEAD~1",
            "git rebase develop",
            "git stash",
            "git config core.hooksPath /dev/null",
            "./gradlew detekt && git merge develop",
        ]

        results = [_guard("coder", _bash(command)) for command in commands]

        self.assertEqual(["deny"] * len(commands), results)

    def test_routine_commands_are_left_to_the_permission_rules(self):
        commands = [
            'git commit -m "Merge the lists and update the config screen"',
            "git checkout -- feature/src/commonMain/kotlin/Feature.kt",
            "git checkout HEAD~1 -- feature/src/commonMain/kotlin/Feature.kt",
            "git log --oneline develop..HEAD",
            "./gradlew :feature:otp:jvmTest",
        ]

        results = [_guard("coder", _bash(command)) for command in commands]

        self.assertEqual([None] * len(commands), results)

    def test_unreadable_input_is_denied(self):

        result = _guard("coder", "not json")

        self.assertEqual("deny", result)


class DemoGuardTests(unittest.TestCase):

    def test_demo_output_is_left_to_the_permission_rules(self):
        with _worktree() as root:

            result = _guard("demo", _write(root / "build/feature-factory/search/demo/index.html", root))

            self.assertIsNone(result)

    def test_tracked_files_are_denied(self):
        with _worktree() as root:

            result = _guard("demo", _write(root / "feature/src/commonMain/kotlin/Feature.kt", root))

            self.assertEqual("deny", result)

    def test_commits_are_denied(self):

        result = _guard("demo", _bash('git commit -m "Add the demo"'))

        self.assertEqual("deny", result)

    def test_temporary_worktree_commands_are_left_to_the_permission_rules(self):
        commands = [
            "git worktree add build/feature-factory/search/before 4fef045",
            "git worktree remove --force build/feature-factory/search/before",
            "python3 scripts/feature-factory/capture.py android --test by.overpas.template.e2e.SearchE2eTest --label after-search --out build/feature-factory/search/demo/media",
        ]

        results = [_guard("demo", _bash(command)) for command in commands]

        self.assertEqual([None] * len(commands), results)


class VerifierGuardTests(unittest.TestCase):

    def test_gate_runner_commands_are_allowed(self):
        commands = [
            "git show 4fef045:scripts/feature-factory/gates.py | python3 - start --base 4fef045 --slug otp-search --run r1a1 integrity detekt unit",
            "git show 4fef045:scripts/feature-factory/gates.py | python3 - start --base 4fef045 --slug otp-search --run r2a3 --allow 'config/detekt/**' integrity",
            "python3 build/feature-factory/otp-search/r1a1/gates.py wait",
        ]

        results = [_guard("verifier", _bash(command)) for command in commands]

        self.assertEqual(["allow"] * len(commands), results)

    def test_other_commands_are_denied(self):
        commands = [
            "ls",
            "python3 build/feature-factory/otp-search/r1a1/gates.py wait; rm -rf build",
            "git show 4fef045:scripts/feature-factory/gates.py | python3 - start --slug x $(touch y)",
            "./gradlew detekt",
        ]

        results = [_guard("verifier", _bash(command)) for command in commands]

        self.assertEqual(["deny"] * len(commands), results)

    def test_other_tools_are_denied(self):

        result = _guard("verifier", {"tool_name": "Read", "tool_input": {"file_path": "/etc/hosts"}, "cwd": "."})

        self.assertEqual("deny", result)


if __name__ == "__main__":
    unittest.main()
