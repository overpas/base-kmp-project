# Feature factory templates

## Business spec

Path: `docs/features/<slug>/business-spec.md`.

```markdown
# <Feature name>

## Goal

<The problem of the user and the result, in 1-3 sentences.>

## Users and flows

- <Who does what, step by step, in the words of the user.>

## Acceptance criteria

1. <A behavior that a person can see and a test can check.>

## Scope

- In: <...>
- Out: <...>

## Edge cases and errors

- <...>

## Security and privacy

- <The data that the feature touches and how to protect it, or "none".>

## Platforms

<Android, iOS, desktop.>

## Design

<Link to the decision board, and links to the design artifacts, or "none".>

- <Screen or state>: <short description>
```

## Plan

Path: the plan file of plan mode or the scratchpad directory, never inside the repository.

```markdown
# <Feature name>: plan

## References

- Business spec: docs/features/<slug>/business-spec.md
- Technical design: docs/features/<slug>/tech-design.md, or none
- Decisions: docs/decisions/NNNN-<title>.md, or none
- Design: <decision board link>, <artifact links>, or none

## Approach

<The shape of the change in a few sentences.>

## Slices

1. <Observable increment>. Test: <file and expected failure>. Code: <files>. Check: `<focused command>`.

## Assumptions

- Assumed: <decision taken without the user>

## Mandatory gates

<Gate IDs from the catalog, with the reason for each gate that is not always mandatory.>

## Protected-path exceptions

<Paths that the user approved for change, or "none".>

## Demo

- ui: added | changed | none
- <Flow>: Android test `<fully.qualified.Class>[#method]`, iOS test `iosAppUITests/<Class>/<testMethod>`.
- <Changed flow>: the E2E tests that drive it on the base commit, for the "before" recording.
```

## Gate catalog

The gate runner is `scripts/feature-factory/gates.py`. It runs the requested gates in this order.

| ID | AGENTS.md level | Mandatory when |
|---|---|---|
| `integrity` | none | always |
| `detekt` | L1 static analysis | code changes |
| `unit` | L2 unit tests, L3 UI tests | code changes |
| `coverage` | L4 test coverage | production code changes |
| `e2e-desktop` | L5 E2E tests | UI is added or changed |
| `e2e-android` | L5 E2E tests | UI is added or changed |
| `e2e-ios` | L5 E2E tests | UI is added or changed |

`integrity` fails when the tree is not clean, when no commit follows the base, when a test file is
deleted or renamed, when a baseline file is added, when a code line adds a suppression, `@Ignore` or
`XCTSkip`, or when one of these paths changes without a protected-path exception:

- `.claude/`, `AGENTS.md`, `CLAUDE.md`, each `.gitignore`, each `gradle.properties`
- `config/detekt/`, `config/git-hooks/`, `scripts/`, `docs/decisions/`
- the coverage, coverage aggregation and static analysis convention plugins
- `docs/features/<slug>/business-spec.md` and `docs/features/<slug>/tech-design.md`

A failed gate skips the gates that depend on it: all gates after `integrity`, `coverage` after
`unit`, and the E2E gates after any other gate.

## Technical design

Path: `docs/features/<slug>/tech-design.md`.

```markdown
# <Feature name>: technical design

## Context

<What the feature needs from the code base.>

## Design

<Modules, data, state, navigation, DI, libraries.>

## Decisions

- [NNNN. <Title>](../../decisions/NNNN-<title>.md)

## Risks

- <...>
```

## ADR

Path: `docs/decisions/NNNN-<title>.md`.

```markdown
# NNNN. <Title>

- Status: accepted
- Date: <YYYY-MM-DD>
- Feature: <slug>

## Context

## Decision

## Alternatives

- <Rejected option from the board>: <why not>

## Consequences
```

## Workflow args

```json
{
  "slug": "<slug>",
  "branch": "<branch>",
  "featureDir": "docs/features/<slug>",
  "baseSha": "<commit of the specs>",
  "plan": "<full plan text>",
  "artifacts": {
    "businessSpec": "docs/features/<slug>/business-spec.md",
    "techDesign": "docs/features/<slug>/tech-design.md",
    "decisions": ["docs/decisions/NNNN-<title>.md"],
    "design": ["<decision board link>", "<artifact link>"]
  },
  "gates": ["integrity", "detekt", "unit", "coverage", "e2e-desktop", "e2e-android", "e2e-ios"],
  "allowProtected": [],
  "ui": "added",
  "demo": {"platforms": ["android", "ios"], "spec": "<the Demo section of the plan>"},
  "limits": {"attempts": 5, "rounds": 3, "stall": 3}
}
```

- `techDesign` is `null` and `decisions` is empty when the step did not happen. `design` holds at
  least the decision board.
- `allowProtected` holds glob patterns from the protected-path exceptions of the plan.
- `limits` is optional: verification attempts per review round, review rounds, and the number of
  identical gate failures in a row that stops the run.
