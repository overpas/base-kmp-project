# core:db

Optional. Holds the local SQLite database through SQLDelight: the schema in `src/commonMain/sqldelight/`,
the driver for each target, and a sample store of named items that shows the pattern — queries opened
lazily on the IO dispatcher and observed as a flow. It contributes its bindings to the app graph, so a
feature reaches the store through injection. On desktop the file lives in the platform's app data
folder, or in the folder the `template.data.dir` system property names.

No feature uses it yet: nothing depends on the module, so its bindings do not reach the graph.

## Using it

1. Replace the `Item` table and the sample store with the project's own.
2. Add `implementation(projects.core.db)` to the feature that needs it and to `shared`.
3. Add the store to the feature's app dependencies.

## Removing it

1. Delete `core/db/`.
2. Delete `include(":core:db")` from `settings.gradle.kts`, the `sqldelight` plugin line from the root
   `build.gradle.kts`, and the `core.db` class excludes from
   `build-logic/src/main/kotlin/code-coverage-aggregation.gradle.kts`.
3. Delete the `sqldelight` version, the `sqldelight-*` libraries and the `sqldelight` plugin from
   `gradle/libs.versions.toml`.
