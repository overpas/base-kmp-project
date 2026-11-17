# AGENTS.md

## General

### Do

- Use ASD-STE100.
- Be concise.
- When presenting any information to the user, when breaking it down, you should use 1-4 base
  elements/points. It pertains to explaining what you did, explaining different options to choose,
  or anything else. Any complex piece of information should consist of 1-4 root level elements,
  which in their turn can be expanded or broken down further, if prompted or required.
- Always start work in a new git branch from `main`, unless the user names another base branch.
  Obey "Git".
- Don't leave any files unstaged and uncommitted. Either stage and commit or add to `.gitignore` if
  it makes sense.
- Before planning a change, read the decision records in `docs/decisions/` that concern it. Write
  records there only in the technical design step of the feature factory.

### Don't

- Write any comments in the code
- Suppress any static analysis findings with `@Suppress` and don't baseline them
- Weaken detekt rules configuration without explicit user request
- Describe or reference the concrete modules, files, classes, functions currently present in this
  project in docs. Refer to them as generic abstractions.

## Setup

- The external skills are plugins from `.claude-plugin/marketplace.json`, pinned to commits. On a
  new machine trust the folder, then run `claude plugin install <skill> --scope project` for each.

## Git

- Work only in a linked worktree under `.claude/worktrees/`, never in the main checkout. Parallel
  sessions in one checkout break each other's branches and uncommitted changes.
- If the session is in the main checkout, call `EnterWorktree` first. If it is already in a
  worktree, stay there and do not create a nested worktree.
- In the worktree, create a new branch from `main` (or the base branch that the user names):
  `git checkout -b <branch> main`.
- One branch per session, unless the user deviates from initial topic - in this case ask the user if
  you should continue working in the same branch or create a new one.
- At the end commit all the work. Keep the branch and report its name. Do not merge it, unless the
  user calls the branch-merging skill.
- Fixes, corrections, improvements belong in the same branch.

## Verification

### L1: Static analysis

```shell
./gradlew detekt # runs automatically on pre-commit
./gradlew lint # android lint, all checks on, warnings are errors
```

### L2: Unit tests

For quick feedback run specific tests instead of all tests.

```shell
./gradlew allTests # unit tests on all targets. no need to run per platform if this passes
./gradlew testAndroidHostTest # android tests
./gradlew jvmTest # jvm tests
./gradlew iosSimulatorArm64Test # ios tests
```

### L3: UI tests

Compose UI tests are required when a UI functionality is added or modified.

Compose UI tests are run with the same commands as the unit tests.

### L4: Test coverage

```shell
./gradlew koverVerify
```

### L5: E2E tests

E2E tests are required when a UI functionality is added or modified.

```shell
./scripts/desktop-e2e.sh # runs the desktop app offscreen on the JVM, needs no device
./scripts/android-e2e-test.sh # boots, configures and stops its own headless emulator
./scripts/ios-e2e.sh # runs on iOS simulator
```

Set `E2E_VISUAL=1` on the Android or iOS script to show the emulator or simulator window during
the run.

The e2e scripts share one device lock across all worktrees, so a second run waits until the first
one ends. Close the emulator/simulator before running the other platform e2e tests because of RAM
constraints.

### Scripts and hooks

Run when `scripts/` or `.claude/hooks/` change; they guard the verification tooling itself.

```shell
python3 -m unittest discover -s scripts/tests
```

## Guideline files

- AGENTS.md, CLAUDE.md, `.claude/agents/`, `.claude/skills/`, `.claude/hooks/`,
  `.claude/workflows/`, `.claude/settings*.json`, `.agents/`, `.skills/`, `.codex/` and
  `.claude-plugin/` are read-only for agents. A PreToolUse hook denies writes to them.
- Do not change a guideline to fit the task. When the code breaks a guideline, fix the code or ask
  the user.
- When a change sets a convention, propose the text in chat. The user runs `/edit-guidelines` to
  allow the edit for one turn.
