import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


_WORKFLOW = Path(__file__).resolve().parents[2] / ".claude/workflows/feature-build.js"
_BASE = "4fef0451"
_HARNESS = """
import { readFileSync } from 'node:fs'
const [scriptPath, scenarioPath] = process.argv.slice(2)
const source = readFileSync(scriptPath, 'utf8').replace('export const meta', 'const meta')
const scenario = JSON.parse(readFileSync(scenarioPath, 'utf8'))
const calls = []
const agent = async (prompt, options) => {
  calls.push({ agentType: options.agentType, label: options.label, phase: options.phase, prompt })
  const queue = scenario.responses[options.agentType] ?? []
  if (!queue.length) throw new Error(`no response left for ${options.agentType}`)
  return queue.shift()
}
const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor
const run = new AsyncFunction('args', 'agent', 'log', 'phase', source)
const result = await run(scenario.args, agent, () => {}, () => {})
console.log(JSON.stringify({ result, calls }))
"""


def _args(**overrides):
    value = {
        "slug": "note-search",
        "branch": "note-search",
        "featureDir": "docs/features/note-search",
        "baseSha": _BASE,
        "plan": "1. Add the search field.",
        "artifacts": {"businessSpec": "docs/features/note-search/business-spec.md"},
        "gates": ["detekt", "integrity", "unit"],
        "allowProtected": [],
        "ui": "added",
        "demo": {"platforms": ["android", "ios"], "spec": "Show the search flow."},
    }
    value.update(overrides)
    return value


def _work(blockers=()):
    return {"summary": "Added the search field.", "commits": ["abc1234 Add the search field"], "blockers": list(blockers)}


def _verification(run, failing=None, missing=(), environment=None):
    failing = failing or {}
    gates = []
    for gate in ("integrity", "detekt", "unit"):
        if gate in missing:
            continue
        if gate == environment:
            gates.append({"id": gate, "status": "env-error", "exitCode": None, "log": None, "fingerprint": "", "excerpt": "no AVD"})
        elif gate in failing:
            gates.append({"id": gate, "status": "fail", "exitCode": 1, "log": f"build/{gate}.log", "fingerprint": failing[gate], "excerpt": f"{gate} broke"})
        else:
            gates.append({"id": gate, "status": "pass", "exitCode": 0, "log": f"build/{gate}.log", "fingerprint": "", "excerpt": ""})
    return {"status": "done", "run": run, "base": _BASE + "0" * 32, "head": "feedface", "gates": gates}


def _review(*severities):
    return {
        "findings": [
            {"standpoint": "intent", "severity": severity, "file": "Search.kt", "line": 3, "problem": f"{severity} problem", "change": "fix it"}
            for severity in severities
        ]
    }


def _demo():
    return {"path": "/repo/.demos/note-search/index.html", "media": ["after-search.mp4"], "highlights": ["Search"], "problems": []}


def _run(args, responses):
    with tempfile.TemporaryDirectory() as directory:
        harness = Path(directory) / "harness.mjs"
        harness.write_text(_HARNESS, encoding="utf-8")
        scenario = Path(directory) / "scenario.json"
        scenario.write_text(json.dumps({"args": args, "responses": responses}), encoding="utf-8")
        completed = subprocess.run(["node", str(harness), str(_WORKFLOW), str(scenario)], capture_output=True, text=True, timeout=60)
        if completed.returncode != 0:
            raise AssertionError(completed.stderr)
        output = json.loads(completed.stdout)
        return output["result"], output["calls"]


@unittest.skipUnless(shutil.which("node"), "node is missing")
class FeatureBuildTests(unittest.TestCase):

    def test_clean_run_implements_verifies_reviews_and_demos(self):

        result, calls = _run(_args(), {"coder": [_work()], "verifier": [_verification("r1a1")], "code-reviewer": [_review()], "demo": [_demo()]})

        self.assertEqual("passed", result["status"])
        self.assertEqual(["implement r1a1", "gates r1a1", "review r1", "demo"], [call["label"] for call in calls])

    def test_start_command_runs_the_base_gate_runner_with_ordered_gates(self):

        result, calls = _run(
            _args(allowProtected=["config/detekt/**"]),
            {"coder": [_work()], "verifier": [_verification("r1a1")], "code-reviewer": [_review()], "demo": [_demo()]},
        )

        self.assertEqual(
            "Start command:\n"
            f"git show {_BASE}:scripts/feature-factory/gates.py | python3 - start --base {_BASE} --slug note-search --run r1a1 "
            "--allow 'config/detekt/**' integrity detekt unit",
            calls[1]["prompt"],
        )

    def test_gate_failure_goes_back_to_the_coder_with_the_excerpt(self):

        result, calls = _run(
            _args(),
            {
                "coder": [_work(), _work()],
                "verifier": [_verification("r1a1", failing={"detekt": "f1"}), _verification("r1a2")],
                "code-reviewer": [_review()],
                "demo": [_demo()],
            },
        )

        self.assertEqual("passed", result["status"])
        self.assertIn("### Gate detekt: fail (exit code 1)\nFull log: build/detekt.log\ndetekt broke", calls[2]["prompt"])

    def test_missing_gate_result_counts_as_failure(self):

        result, calls = _run(
            _args(limits={"attempts": 1, "rounds": 1, "stall": 3}),
            {"coder": [_work()], "verifier": [_verification("r1a1", missing=("unit",))]},
        )

        self.assertEqual(("failed", "verification-exhausted"), (result["status"], result["reason"]))
        self.assertIn("### Gate unit: no result", result["detail"])

    def test_same_failure_three_times_stops_as_stalled(self):

        result, calls = _run(
            _args(),
            {
                "coder": [_work(), _work(), _work()],
                "verifier": [_verification(f"r1a{attempt}", failing={"unit": "same"}) for attempt in (1, 2, 3)],
            },
        )

        self.assertEqual(("failed", "stalled"), (result["status"], result["reason"]))
        self.assertEqual(6, len(calls))

    def test_changing_failures_stop_after_the_attempt_limit(self):

        result, calls = _run(
            _args(limits={"attempts": 2, "rounds": 3, "stall": 3}),
            {
                "coder": [_work(), _work()],
                "verifier": [_verification("r1a1", failing={"unit": "one"}), _verification("r1a2", failing={"unit": "two"})],
            },
        )

        self.assertEqual(("failed", "verification-exhausted"), (result["status"], result["reason"]))

    def test_environment_error_stops_the_run(self):

        result, calls = _run(_args(), {"coder": [_work()], "verifier": [_verification("r1a1", environment="unit")]})

        self.assertEqual(("failed", "environment"), (result["status"], result["reason"]))
        self.assertEqual(2, len(calls))

    def test_result_for_another_run_is_rejected(self):

        result, calls = _run(_args(), {"coder": [_work()], "verifier": [_verification("r9a9")]})

        self.assertEqual(("failed", "environment"), (result["status"], result["reason"]))

    def test_blocker_stops_before_verification(self):

        result, calls = _run(_args(), {"coder": [_work(blockers=["The plan needs a new library."])]})

        self.assertEqual(("failed", "blocked", ["The plan needs a new library."]), (result["status"], result["reason"], result["detail"]))
        self.assertEqual(["implement r1a1"], [call["label"] for call in calls])

    def test_blocking_review_finding_goes_back_to_the_coder(self):

        result, calls = _run(
            _args(),
            {
                "coder": [_work(), _work()],
                "verifier": [_verification("r1a1"), _verification("r2a1")],
                "code-reviewer": [_review("blocking", "nit"), _review("nit")],
                "demo": [_demo()],
            },
        )

        self.assertEqual("passed", result["status"])
        self.assertEqual(["nit problem"], [nit["problem"] for nit in result["nits"]])
        self.assertIn("1. [intent] Search.kt:3: blocking problem\n   Required change: fix it", calls[3]["prompt"])
        self.assertIn("## Earlier blocking findings", calls[5]["prompt"])

    def test_blocking_findings_in_every_round_stop_the_run(self):

        result, calls = _run(
            _args(limits={"attempts": 5, "rounds": 2, "stall": 3}),
            {
                "coder": [_work(), _work()],
                "verifier": [_verification("r1a1"), _verification("r2a1")],
                "code-reviewer": [_review("blocking"), _review("blocking")],
            },
        )

        self.assertEqual(("failed", "review-exhausted"), (result["status"], result["reason"]))

    def test_invalid_args_are_reported_before_any_agent(self):

        result, calls = _run(_args(gates=["detekt", "lint"], ui="maybe", baseSha="develop"), {})

        self.assertEqual("error", result["status"])
        self.assertEqual(
            [
                "baseSha must be a commit hash",
                "unknown gates: lint",
                "gates must include integrity",
                "ui must be one of added, changed, none",
            ],
            result["problems"],
        )
        self.assertEqual([], calls)


if __name__ == "__main__":
    unittest.main()
