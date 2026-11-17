# Architecture

This is a **Kotlin Multiplatform** project with **Compose Multiplatform** for shared UI across
Android, iOS, and Desktop.

```
<project>/
├── shared/                      # app shell: App(), root navigation (Decompose), root DI graph
├── core/                        # reusable libraries
│   ├── arch/                    # UDF primitives: handler types, executor, main-thread state scope
│   ├── db/                      # optional: SQLite schema and drivers through SQLDelight
│   ├── design/                  # design system: themes, typography, icons, reusable UI components
│   ├── platform/                # current platform and per-target services
│   ├── strings/                 # all user-visible strings as Compose resources
│   ├── system/                  # current time provider, replaceable in tests
│   ├── test/                    # unit-test helpers: assertions, flow collection
│   ├── ui-test/                 # Compose UI test base, Ultron config, matchers, e2e entry point
│   │   ├── camera/              # optional: emulator back camera image for Android e2e tests
│   │   └── gallery/             # optional: gallery images and the system photo picker in e2e tests
│   └── ...
│
├── feature/                     # one module per feature, one screen or flow each
│   └── <feature>/               # the feature and its UI
│       ├── tags/                # test tags, exported to the iOS framework
│       └── ui-test/             # page objects of the feature
├── androidApp/                  # Android entry point: MainActivity → App()
├── desktopApp/                  # Desktop entry point: main → Window → App()
│   └── src/e2eTest/             # desktop e2e tests on the real app, offscreen
├── iosApp/                      # Xcode project, entry point calls MainViewController()
│   └── iosAppUITests/           # XCUITest e2e tests
├── build-logic/                 # convention plugins: kmp-lib.gradle.kts, …
└── gradle/
    └── libs.versions.toml       # single source of truth for all dependency versions
```

## Dependencies

Cross-feature dependencies must be avoided. Shared logic or infrastructure between features belongs
to core modules.

## Navigation

Navigation is the process of modifying the currently displayed destinations. Destination is any
distinct piece of UI that helps interact with business logic, typically a screen. However, bottom
sheets, dialogs, etc., can also be distinct destinations. This implies that appearance of a screen
or disappearance of a screen is always a navigation event.

To handle user navigation to certain UI elements/screens/bottom sheets/dialogs and back use
`Navigator` concept. The `Navigator` interface is typically declared in feature dependencies, and
the implementation is provided by the parent feature/module that uses this feature. The
implementation is backed by Decompose's StackNavigation.

Navigator example:

```kotlin
@Inject
@ContributesBinding(RootScope::class)
internal class RootFeatureXNavigator(
    private val stackNavigation: StackNavigation<Config>,
) : FeatureXNavigator {

    override fun navigateToY() {
        stackNavigation.bringToFront(Config.Y)
    }
}
```

## Feature

A feature is three Gradle modules — the feature itself, a nested `tags` module (test tags) and a
nested `ui-test` module (page objects).

```
feature/<path>/
├── src/
│   ├── commonMain/…/<package>/
│   │   ├── <Feature>Component.kt       # component interface, default and preview implementations
│   │   ├── <Feature>State.kt           # the UI state
│   │   ├── <Feature>Navigator.kt       # outward navigation the parent implements
│   │   ├── <x>/                        # one package per store, named by its responsibility
│   │   │   ├── <X>Store.kt             # Intent, Action, State, Label, Message, reducer of the store
│   │   │   ├── handlers/               # one handler per Intent and per Action of the store
│   │   │   └── …                       # factories, models and mappers that only this store uses
│   │   ├── di/                         # scope, dependency interfaces, bindings, graph
│   │   └── ui/
│   │       ├── <Feature>Ui.kt          # the composable UI of the feature
│   │       ├── <Feature>Components.kt  # internal composables and type-to-text mappings of that UI
│   │       └── <destination>/          # one package per further screen, sheet or dialog, if any
│   ├── commonTest/                     # handler, component and pure-logic tests
│   └── uiTest/                         # UI tests and their fakes
├── tags/…/ui/
│   └── <Feature>Tags.kt                # test tags, exported to the iOS framework
└── ui-test/…/ui/
    └── <Feature>Page.kt                # page object of the feature
```

Four parts, in one **unidirectional data flow (UDF)** loop: UI → component → store → UI.

- **UI** — stateless composables. It reads one state object and reports user events. It holds no
  logic and no navigation.
- **Component** — the feature's Decompose entry point. It owns the stores, exposes one UI state, and
  takes the UI events. It is the only part with a platform lifecycle. No feature logic here, it
  belongs to Store. A component constructor takes only its `ComponentContext`, store factories,
  shared stores, navigators and dispatchers. A collaborator of any other type is feature logic that
  belongs in the store or in one of its handlers. A root child builder may split a screen into
  sibling components. Siblings never hold references or callbacks to each other: they communicate
  only through shared stores.
- **Store** — one **MVI** unit: `Intent` in, `Message` to a reducer, `State` out, `Label` for
  one-shot effects, `Action` for work the store starts itself. An `IntentHandler` holds the logic of
  one intent, an `ActionHandler` the logic of one action; both are the only place where a store
  touches the outside world. All feature logic must be implemented in the Store and the dependencies
  it uses.
- **Navigator** — the interface for leaving the feature. The parent implements it. Used by
  Component.

A component is not tied to one store. It owns as many stores as it has independent responsibilities,
and the UI state it exposes is its own type — a view of one, several or no store states. Never
expose a store state to the UI.

A component has at most 1 observable state property and at most 1 observable events (one-off)
property. No other public properties exist in a component.

Organize a feature into packages by meaning. Each store has its own package with its handlers and
the types that belong to it; the feature package keeps only the component, its UI state and its
navigator. A flat package of several stores hides which file belongs to which responsibility.

Name the types of stores, components and UI states by meaning, not by the widget that shows them:
a notification, not a toast or a snackbar. Only the UI layer names the widget.

Add only what a feature needs: no navigation - no `Navigator`; no user input to check - no
validators or error types.

Build all of this on `:core:arch`, which holds the primitives and helpers of this architecture — the
handler types, the executor that dispatches to them, the component scope and state helpers. Do not
write a feature-local copy of anything it already gives you.

MviKotlin permits state changes on the main thread only. Handlers therefore work in a
`SafeCoroutineExecutorScope` from `:core:arch`, which reads the state and moves every dispatch,
forward and publish onto the main thread. Do the work on any dispatcher you need, then go through
that scope to change the state.

Input that must survive process death goes into a separate serializable type with only the kept
fields: the component consumes it from its `StateKeeper`, gives it to the store factory as the
initial state and registers a supplier for it. Secrets and errors never go into it.

### Feature UI

Feature UI has one or two files in the `ui` package:

- `<Feature>Ui.kt` — the public entry composable. It collects the component state and gives it, with
  the component callbacks, to one private stateless `<Feature>Content`, the layout skeleton.
- `<Feature>Components.kt` — optional. `internal` composables that set up one design system
  component with feature data (tags, texts, allowed values), and mappings from feature types to
  text.

A composable with many callbacks takes them grouped in a `@Stable` `<Name>Actions` data class,
which nests one such class per child composable that takes them.

A feature that shows more than one destination (screen, sheet, dialog) puts each further
destination in its own package `ui/<destination>/`, with one `<Destination>Ui.kt` and an optional
`<Destination>Components.kt`. `<Feature>Ui.kt` stays in `ui/` as the entry that switches between
them.

The design system is Material3 plus `:core:design`. Feature UI calls and combines its components. It
never sets how they look. When a feature needs a look that the design system does not give, add it
to `:core:design`.

| Concern         | Design system                                                   | Feature UI                                                                         |
|-----------------|-----------------------------------------------------------------|------------------------------------------------------------------------------------|
| Color           | Color scheme, `Color(…)`, component colors                      | Picks a component or variant (`Button` or `TextButton`), never a color             |
| Typography      | `TextStyle`, `FontFamily`, `fontSize`                           | Picks a typography role, sets `TextAlign` and `TextOverflow`, never builds a style |
| Dimensions      | dp and sp values as `object` properties in `:core:design`       | Uses those properties for padding and spacing, never a `dp`/`sp` literal           |
| Shape           | `RoundedCornerShape`, `shadow`, `border`, `clip`                | —                                                                                  |
| Drawing         | `androidx.compose.ui.graphics.*`, `androidx.compose.ui.draw.*`  | —                                                                                  |
| Layout          | Custom layouts from `androidx.compose.ui.layout.*`              | Arranges with `androidx.compose.foundation.layout.*` and `verticalScroll`          |
| Icons           | Glyphs as XML resources in `:core:design`                       | Picks the icon, supplies its content description                                   |
| Animation       | Durations, easing, enter and exit transitions                   | Shows or hides content from state                                                  |
| Input kind      | Mask character, keyboard type, line limit, error look of a kind | Picks the kind: secret, number, multiline                                          |
| Text            | Takes text as `String` parameters, holds no strings             | Calls `stringResource`, maps feature types and errors to text                      |
| Test tags       | Takes a tag parameter for each inner node                       | Supplies values from `<Feature>Tags`, tags its own nodes                           |
| Events          | Exposes callbacks: `onClick`, `onValueChange`                   | Connects them to component methods, with no logic inside                           |
| Component state | Shows the state it gets: `enabled`, `isError`, `expanded`       | Supplies it from the UI state, e.g. `enabled = state.isSaveEnabled`                |
