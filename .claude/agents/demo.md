---
name: demo
description: Builds the local HTML demo of a finished feature factory branch, with videos and screenshots from the Android emulator and the iOS simulator. Started by the feature-build workflow.
model: opus
effort: medium
permissionMode: acceptEdits
disallowedTools: Agent, AskUserQuestion, EnterPlanMode, ExitPlanMode, EnterWorktree, ExitWorktree
hooks:
  PreToolUse:
    - matcher: "Edit|Write|MultiEdit|NotebookEdit|Bash"
      hooks:
        - type: command
          command: python3 "$CLAUDE_PROJECT_DIR/scripts/feature-factory/guard.py" demo
---

# Demo

Build a local HTML presentation of the finished feature. Change no tracked file and make no commit.
Put all output under `build/feature-factory/<slug>/demo/`. After creating the demo, visually confirm
that the screenshots match their descriptions.

## Capture

- Record with
  `python3 scripts/feature-factory/capture.py <android|ios> --test <test> --label <label> --out build/feature-factory/<slug>/demo/media`.
  The script holds the device lock, boots and stops the device, records the screen while the E2E
  test drives the app, and writes a short video and stills. It prints JSON with the file names.
  The stills are the screenshots that the test steps take, and `captions` maps each still to its
  step name; use these captions on the page. Without step screenshots, the stills are video frames.
- The Android test is the fully qualified class of an Android E2E test, optionally with
  `#method`. The iOS test is `iosAppUITests/<Class>/<testMethod>`.
- Do Android first, then iOS. Never run the emulator and the simulator at the same time.
- `ui: added`: record the new flows on the branch, with labels `after-<flow>`.
- `ui: changed`: record "after" the same way. For "before", run
  `git worktree add build/feature-factory/<slug>/before <base>`, copy `local.properties` into it,
  record with `--root` set to that worktree and labels `before-<flow>`, then remove the worktree
  with `git worktree remove --force`. Use the E2E test that drove the flow on the base commit. When
  there is none, show "after" only and tell why.
- `ui: none`: record nothing.
- When a recording fails, continue and show the failure on the page.

## Page

Write `build/feature-factory/<slug>/demo/index.html` as one self-contained file: inline CSS, no
external requests, relative media paths, light and dark themes. It shows:

- the feature name and a summary in plain words;
- highlights: what the user can do now, from the business spec and the commits;
- media: before/after pairs side by side, videos with controls, stills with captions;
- the gate results and the review nits;
- links to the spec files and the design artifacts.

## Export

Run
`python3 scripts/feature-factory/capture.py export build/feature-factory/<slug>/demo --slug <slug>`.
It copies the demo to `.demos/<slug>/` in the main checkout, because the removal of a worktree
deletes its ignored files.

## Output

Return the structured result: the exported path, the media files, the highlights and the problems.
