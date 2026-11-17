# Template

A base for Kotlin Multiplatform apps on Android, iOS and desktop, meant to be used as a GitHub
template. It holds the build setup, the architecture primitives, the test and end-to-end tooling,
and an agent workflow, plus two small sample screens that exercise all of it.

The user interface is shared across all three targets with Compose Multiplatform. Navigation is
Decompose, each feature is a unidirectional data flow over MviKotlin, and dependencies are wired
with Metro.

## Using this template

1. Create a repository from the template, clone it, and rename the project:

   ```bash
   python3 scripts/rename-project.py --package com.example.notes --name notes --app-name Notes
   ```

2. Set the git hooks path, so detekt runs before each commit:

   ```bash
   git config --local core.hooksPath config/git-hooks
   ```

3. Install the agent plugins the guidelines need (see `AGENTS.md`, section Setup).
4. Replace the sample features `feature/hello` and `feature/greeting` with your own, and their
   documents in `docs/features/`.
5. Keep or remove the optional modules. Each README says how to remove it:
   - `core/db`: a local SQLite database through SQLDelight.
   - `core/ui-test/camera`: Android end-to-end tests that choose what the emulator camera shows.
   - `core/ui-test/gallery`: end-to-end tests that pick images in the system photo picker.
6. Put your design system in `core/design`. It ships only a Material 3 theme.

## Layout

| Path | Holds |
|---|---|
| `shared/` | the application shell: root composable, root navigation, the aggregating graph |
| `core/` | capabilities several features share — see each module's own README |
| `feature/` | one screen or flow each, with its test tags and page objects beside it |
| `androidApp/`, `desktopApp/`, `iosApp/` | the three entry points |
| `build-logic/` | the convention plugins every module applies |
| `config/` | detekt rules and plugins, Android lint, git hooks |
| `docs/` | decision records, feature specifications and technical designs, investigations |
| `scripts/` | end-to-end runners, the feature factory gates, the rename script |
| `.claude/`, `.claude-plugin/`, `AGENTS.md` | the agent guidelines, skills, sub-agents and workflow |

## Running

```bash
./gradlew :androidApp:installDebug     # Android, onto a running device or emulator
./gradlew :desktopApp:run              # desktop
./gradlew :desktopApp:hotRun --auto    # desktop, with hot reload
```

For iOS, open `iosApp` in Xcode and run it there.

## Testing

```bash
./gradlew detekt lint                  # static analysis
./gradlew allTests                     # every module, every runnable target
./gradlew :shared:jvmTest              # fastest feedback on shared logic
./gradlew koverVerify                  # fails under 80% line coverage

./scripts/desktop-e2e.sh               # desktop end-to-end tests, with an Allure report
./scripts/android-e2e-test.sh          # Android end-to-end tests, with an Allure report
./scripts/ios-e2e.sh                   # iOS end-to-end tests, with an Allure report

python3 -m unittest discover -s scripts/tests   # the scripts and hooks themselves
```

The end-to-end scripts need [Allure](https://allurereport.org/docs/install/), and the Android SDK
or Xcode. GitHub Actions runs all of these on every pull request, in parallel jobs: the iOS work on
macOS runners, the rest on Linux runners. Each end-to-end job keeps its Allure report as an
artifact. The `CI passed` job fails when one of the other jobs fails, so it can be the one
required check.
