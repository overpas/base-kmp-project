# Testing

Arrange-Act-Assert pattern. newlines between the blocks in unit tests Use /testing-on-the-toilet:testing-on-the-toilet
skill. When testing compose code also use /chrisbanes-skills:compose-ui-testing-patterns skill.

## Running tests

Prefer `./gradlew :shared:jvmTest` for rapid feedback on shared logic changes; run
`./gradlew allTests` before pushing.

Never run `./gradlew build`. It compiles every iOS target and exhausts the daemon heap on this
machine, so it does not finish; run the narrow per-target tasks instead, one at a time. The Android
emulator and the iOS simulator must never run at the same time — shut one down and confirm it is
gone before the other starts.

## Where a test goes

Two test directories per module:

- `src/uiTest/` — **Compose UI tests only**, plus their fakes. These run on every target and on a
  device, so keep them free of platform code.
- `src/commonTest/` — everything else. Never dexed, never runs on a device.

End-to-end tests are the exception and live outside both: `androidApp/src/androidTest/` on Android,
`iosApp/iosAppUITests/` on iOS, `desktopApp/src/e2eTest/` on desktop.

## Page objects

Every Compose UI test drives the UI through a page object.

Each feature keeps its page objects in a nested module beside it — `:feature:<feature>:ui-test` — in
`commonMain`, in the same `…ui` package as the UI they drive. A core module whose page objects only
its own tests use keeps them in its `src/uiTest/`, with each test tag in its page object.

There is exactly one page object per piece of UI, used by both. Never write a second page object for
the same UI. A Swift page object for the iOS end-to-end tests is not a second one: it is the same
page object on a toolchain that cannot consume Kotlin test code, and it mirrors its method names.

A page object is an `object`, not a class — Ultron's operations find the test environment on their
own, so nothing needs to be passed in. The single exception is `setContent`, declared as a
`ComposeUiTest` extension inside the object:

```kotlin
object NotesPage {

    private val addButton = hasTestTag(NotesTags.ADD_BUTTON)

    @OptIn(ExperimentalTestApi::class)
    fun ComposeUiTest.setContent(component: NotesComponent) = setContent {
        NotesUi(component = component, modifier = Modifier.fillMaxSize())
    }

    fun clickAddButton() {
        addButton.click()
    }
}
```

## End-to-end tests

E2e test must be on point and test the minimum required scenario. Avoid repeating the steps that are
already covered by other tests.

An iOS end-to-end test mirrors its Android end-to-end test: the same scenario against the same app,
with the same step names. A test that needs the back camera has no iOS counterpart, because the iOS
simulator has no camera.

A desktop end-to-end test mirrors its Android end-to-end test in the same way. A test that needs the
camera, system back or the system picker has no desktop counterpart, and Escape is the desktop back
key. Each desktop test runs the real app offscreen on an empty JUnit `@TempDir` data directory, so
it never touches the user's desktop data.

Name each phase of a test with `step("…") { }` from `:core:ui-test` on Android and desktop and from the XCUITest
`step` shim on iOS, not Ultron's: only they attach the end-of-phase screenshot to the report. End
each action step with an assertion that waits for the action's visible result, such as the next
screen, menu or dialog: the app reacts on background dispatchers that neither Compose idling nor
XCUITest waits for, so a step without it can end, and be shot, too early.

If the project keeps `:core:ui-test:camera`, mock the back camera image of an Android e2e test with
the `BackCameraImage` rule, or with `BackCamera.show` inside a step.

If the project keeps `:core:ui-test:gallery`, supply the image a test picks from the gallery with the
`GalleryImage` rule and pick it with `PhotoPicker.pickNewestImage()` on Android, and with
`GalleryImage.add`, `GalleryImage.remove` and `PhotoPicker.pickNewestImage()` from
`iosApp/UITestSupport/` on iOS.

To run the e2e scripts for another app in the repository, add a thin wrapper that sets the app's
directory and IDs, not a copy of the script.

## Test method naming

Use `` `backticked names with spaces` `` freely — except in `src/uiTest/`,
`androidApp/src/androidTest/` and `desktopApp/src/e2eTest/`, where methods must be camelCase.

A Swift test method carries the `test` prefix XCTest discovers it by, and keeps the Kotlin name
after it: `noteIsDisplayedAfterItIsSaved` becomes `testNoteIsDisplayedAfterItIsSaved`.

## Ultron drives every UI interaction

UI tests and page objects use [Ultron](https://open-tool.github.io/ultron/)
(`libs.ultron.compose`), never the raw Compose test API. A locator is a plain
`SemanticsMatcher` — `hasTestTag(SomeTags.FIELD)` — and every operation hangs straight off it:

```kotlin
private val saveButton = hasTestTag(NoteEditorTags.SAVE_BUTTON)

fun clickSave() {
    saveButton.click()
}
fun assertSaveButtonIsEnabled() {
    saveButton.assertIsEnabled()
}
fun assertNameEquals(name: String) {
    nameField.assertTextEquals(name)
}
fun assertNameErrorIsNotDisplayed() {
    nameFieldError.assertDoesNotExist()
}
```

Use the Ultron name for an operation, not the Compose one: `click`, `typeText`, `replaceText`,
`clearText`, `scrollTo`, `setSelection`, `assertMatches`, `getNodeConfigProperty`.

Start a UI test with `runUltronUiTest { }`, not `runComposeUiTest { }`, and an e2e test with
`runE2eTest<MainActivity> { }` (Android) or `runE2eTest(content = { DesktopApp(appDataDir) }) { }`
(desktop) from `:core:ui-test`: Ultron's operations need the test environment
that they install.

## No helper functions in tests

Never define helper functions inside a test class — no builders/factories with default arguments, no
shared setup functions. A test case must be readable top to bottom without jumping to another
function to learn what the data actually is. Verbosity and duplication are the accepted cost; do not
"clean it up" into helpers later.

## Test data

Never create test data with the API under test: the test then checks the code against itself.

## Where tests are not needed

- Generated code, DI setup code, and other code excluded from test coverage.
- Simple classes that serve the purpose of a "Factory", which simply redirects to other creator
  class or function.
- Classes and functions made for preview, usually having "preview" in their name.
- Top level and `object` variables and constants
- Classes that fulfill the "helper" role for testing purposes.
