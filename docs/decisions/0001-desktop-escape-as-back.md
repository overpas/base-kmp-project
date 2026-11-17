# 0001. Escape is back on desktop

- Status: accepted
- Date: 2026-11-17
- Feature: hello-greeting

## Context

Desktop has no system back. Menus, sheets and pushed screens must close on the Escape key, as they
close on back on Android and iOS. Key handling does not belong in the design system. Without a
back dispatcher in the desktop root component context, the back callbacks of the components never
fire on desktop.

## Decision

`desktopApp` gives the root component context a `BackDispatcher` and calls `back()` on Escape from a
key handler around `App`. The app entry point and the desktop E2E host use the same handler.

## Alternatives

- Escape in each design system component: key handling in the design system.
- Escape only on the screens that show a menu: other screens keep no Escape.

## Consequences

- Escape leaves every pushed screen and closes sheets on desktop, like back on Android.
