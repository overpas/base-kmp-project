# 0002. Saved input for process death

- Status: accepted
- Date: 2026-11-17
- Feature: hello-greeting

## Context

A screen with user input must give that input back after the system stops the app. Stores live in
the instance keeper, so they survive a configuration change but not process death. All three apps
give the root component a state keeper.

## Decision

- A feature keeps input for process death in a separate serializable type that holds only the kept
  fields. The component consumes it from the Decompose state keeper, gives it to the store factory
  as the initial state, and registers a supplier that maps the current store state to it.
- Secrets, validation errors and one-shot results are never part of that type.
- The feature module applies the Kotlin serialization plugin for that type.
- The desktop app writes its saved state through the state saver when the window closes, so the
  navigation stack and the saved input come back after a restart.

## Alternatives

- Serializable store state with transient fields: one missing annotation writes a secret to disk,
  and the store state becomes a storage format.
- No desktop save: desktop would lose the input on every restart.

## Consequences

- On desktop the saved input and the navigation stack are written unencrypted to the saved state
  file in the working folder.
- Android and iOS keep the saved state in the system saved-state storage of the app.
- Every feature that keeps input follows the same shape.
