# Gradle configs

## Use implementation() by default

Use `implementation()` instead of `api()`. The only exception feeds the iOS framework: a dependency
the same script passes to `export()` is declared with `api()`, as `:shared` does for the `tags`
modules, Decompose and Essenty.

## Extract common configuration to convention plugins

Each convention plugin should be responsible for one thing, e.g. static analysis, code coverage,
etc.

## Sorting

Applied plugins: `id()` plugins first, then `alias()` plugins, each group sorted alphabetically.
Exception: a convention plugin keeps a plugin that another one needs first (`kmp-di` applies
Kotlin before Metro).

The targets' dependencies blocks must be sorted alphabetically by target name.

The dependencies of one configuration stay together. Inside it, the order is projects, libs,
test-only libs, and each group is sorted alphabetically.
