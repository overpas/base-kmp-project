# Hello and greeting

## Goal

Show the template's architecture end to end with the smallest real flow: input, a store, navigation
with an argument, back, and saved input.

## Users and flows

- Step: the user opens the app and sees the hello screen with an empty name field.
- Step: the user types a name. The Greet button turns on once the name is not blank.
- Step: the user clicks Greet. The greeting screen opens and greets the name.
- Step: the user clicks Back, presses system back, or presses Escape on desktop. The hello screen
  shows again with the name still typed.

## Acceptance criteria

1. The hello screen shows the title "Hello", a "Your name" field and a "Greet" button.
2. Greet is off while the name is empty or blank.
3. Greet opens the greeting screen with "Hello, <name>!", the name trimmed.
4. Back on the greeting screen returns to the hello screen with the name kept.
5. The typed name comes back after process death.

## Scope

- In: the two screens, navigation between them, saved input.
- Out: storage of names, validation beyond blank.

## Edge cases and errors

- A name of spaces only keeps Greet off.

## Security and privacy

The name stays on the device. On desktop it is written unencrypted to the saved state file.

## Platforms

Android, iOS and desktop, with the same behavior. Escape is back on desktop only.

## Design

Material 3 defaults through the template theme. No custom design.
