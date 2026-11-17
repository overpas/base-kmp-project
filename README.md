# mauth

A one-time-password authenticator for Android, iOS and desktop. Entries are added by hand, kept in a
local database, and shown as codes that refresh as time passes. Nothing leaves the device.

The user interface is shared across all three targets with Compose Multiplatform. Navigation is
Decompose, each feature is a unidirectional data flow over MviKotlin, dependencies are wired with
Metro, and entries are stored through SQLDelight.

## Layout

| Path | Holds |
|---|---|
| `shared/` | the application shell: root composable, root navigation, the aggregating graph |
| `core/` | capabilities several features share — see each module's own README |
| `feature/` | one screen or flow each, with its test tags and page objects beside it |
| `androidApp/`, `desktopApp/`, `iosApp/` | the three entry points |
| `build-logic/` | the convention plugins every module applies |

## Prerequisites

Set the git hooks path to `config/git-hooks` for git hooks to work:

```bash
git config --local core.hooksPath config/git-hooks
```

## Running

```bash
./gradlew :androidApp:installDebug     # Android, onto a running device or emulator
./gradlew :desktopApp:run              # desktop
./gradlew :desktopApp:hotRun --auto    # desktop, with hot reload
```

For iOS, open `iosApp` in Xcode and run it there.

## Testing

```bash
./gradlew allTests                     # every module, every runnable target
./gradlew :shared:jvmTest              # fastest feedback on shared logic
./gradlew koverVerify                  # fails under 80% line coverage

./scripts/desktop-e2e.sh               # desktop end-to-end tests, with an Allure report
./scripts/android-e2e-test.sh          # Android end-to-end tests, with an Allure report
./scripts/ios-e2e.sh                   # iOS end-to-end tests, with an Allure report
```

## Security

Secrets never leave the device and are excluded from platform backup. They are held unencrypted at
rest on all three targets; encrypting them, keyed by the platform secure store, is tracked work and
is not yet done.
