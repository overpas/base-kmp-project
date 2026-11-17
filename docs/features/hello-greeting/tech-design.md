# Hello and greeting: technical design

## Context

The template has the app shell, the root navigation and the architecture primitives, but no feature
that uses them.

## Design

- Module: two feature modules, one per screen, each with its `tags` and `ui-test` modules.
- Design system: Material 3 components and the spacing values of the design module.
- Store: the hello screen has one store that holds the name and whether Greet is on. Name changes go
  through an intent handler, the Greet check through an action handler that the bootstrapper also
  starts, so a restored name turns Greet on. The greeting screen has no store: it holds no logic.
- Saved input: the hello screen keeps the name in a separate serializable type.
- UI state: each screen exposes one state type of its own.
- Navigation: the hello navigator opens the greeting with the name; the greeting navigator goes back.
  The root pushes a greeting configuration that carries the name, so the root child builders get
  the configuration they build.
- Desktop: Escape is back.

## Decisions

- [0001. Escape is back on desktop](../../decisions/0001-desktop-escape-as-back.md)
- [0002. Saved input for process death](../../decisions/0002-saved-input-for-process-death.md)

## Risks

- None beyond those of the template itself.
