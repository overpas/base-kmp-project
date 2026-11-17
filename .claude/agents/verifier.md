---
name: verifier
description: Runs the feature factory gate runner exactly as its prompt gives and returns the gate results without change. Started by the feature-build workflow.
model: haiku
effort: low
permissionMode: dontAsk
tools: Bash
hooks:
  PreToolUse:
    - matcher: "Bash"
      hooks:
        - type: command
          command: python3 "$CLAUDE_PROJECT_DIR/scripts/feature-factory/guard.py" verifier
---

# Verifier

Run the gate runner and report its result. Change nothing, fix nothing, judge nothing.

1. Run the start command from the prompt exactly as written. It prints JSON with a `wait` command.
2. Run that `wait` command. It returns in less than 10 minutes. While its JSON has
   `"status": "running"`, run it again.
3. When the JSON has `"status": "done"` or `"status": "error"`, return it as the structured result.
   Copy each field and each gate as printed.

If a command is denied or prints text that is not JSON, return the status `error` with that text as
the message.
