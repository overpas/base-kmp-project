---
name: code-conventions
description: MANDATORY for any work on this project. Invoke this skill FIRST, before reading code, planning or answering, whenever the task touches the project in any way - developing new functionality, fixing bugs, debugging, refactoring, writing or running tests, reviewing code or a PR, making architectural or design decisions, adding or wiring modules, editing build files, Gradle config, static analysis, CI, scripts or other project infrastructure, adding strings or resources, or discussing, explaining or planning any of these. It holds the conventions every change must meet - code, tests, architecture, Gradle and resources. If in doubt, invoke it.
---

# Code conventions

The rules any work on this project must abide by.

## General

- Keep changes minimal as long as it satisfies a goal.
- Write no comments in the code.

## What to read

| If you are about to                                                                                                      | Read                           |
|--------------------------------------------------------------------------------------------------------------------------|--------------------------------|
| Write or change code                                                                                                     | `references/coding.md`         |
| Write, place, or run a test                                                                                              | `references/testing.md`        |
| Add or change behavior: a user action, UI state, navigation, a data source or storage, DI wiring, or where new code goes | `references/architecture.md`   |
| Edit a gradle config file                                                                                                | `references/gradle-configs.md` |
| Add a string, a test tag, or an icon glyph, or other type of resources                                                   | `references/resources.md`      |

## Required plugin skills

- The references name skills as `/<plugin>:<skill>`. They come from the plugins in
  `.claude-plugin/marketplace.json`. When one does not load, stop the work and tell the user to run
  `claude plugin install <plugin>@template --scope project` and start a new session.
