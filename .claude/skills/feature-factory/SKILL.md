---
name: feature-factory
description: Run the feature factory for a new feature - settle each decision with the user on a visual decision board, write the business spec, an optional UI/UX design in Claude Design, a plan with mandatory verification gates, an optional technical design with decision records, then hand off to the feature-build workflow, which implements, verifies, reviews and demos the feature without the user. Use only when the user calls /feature-factory.
argument-hint: "<feature intent>"
disable-model-invocation: true
---

# Feature factory

Take a feature intent to a reviewed, committed branch and a local demo. Steps 1 to 4 need the user.
After the hand-off in step 5, no step asks the user anything.

## Terms

- `<slug>`: the feature name in lowercase words joined by hyphens, made from the intent.
- `<feature dir>`: `docs/features/<slug>/`.
- Templates: [references/templates.md](references/templates.md). They hold the spec, plan, technical
  design and ADR layouts, the gate catalog and the workflow `args`.
- Board: the decision board of the feature, one artifact. Read
  [references/decision-board.md](references/decision-board.md) before the first card.

## Decisions

- Each decision of the user goes on the board with its context, the real options and a
  recommendation. The user picks an option or writes feedback, and the rounds continue until every
  decision is settled.
- Use AskUserQuestion only for a one-line process question that needs no context.

## 0. Set up

- Invoke `code-conventions`.
- Obey the Git rules of AGENTS.md: enter a worktree if the session is in the main checkout, then
  create the branch `<slug>` from `main`, or from the base that the user names.

## 1. Functionality brainstorm

- Research first. Find facts in the code instead of asking for them.
- Find the open decisions on the goal and the users, the user flows, acceptance criteria that a test
  can check, scope and non-goals, edge cases and errors, security and privacy, and platforms.
- Publish the board and settle the decisions with `phase: brainstorm`.
- Write `<feature dir>/business-spec.md` from the template in non-technical words: no modules,
  classes or libraries. Put the spec on the board as a `review` decision, and continue when the user
  approves it.

## 2. UI/UX design (optional)

- Each UI decision is a `ui` decision on the board, with mockups of the options.
- When the feature adds a new screen or a new flow, ask the user if a full design is necessary after
  the board settles the UI decisions.
- Default tool: a Claude Design artifact. Run the Artifact `quickstart` with intent `design`, ask
  which listed design system to use, create the artifact from the Design type and follow its
  instructions. Other connected design tools are also permitted.
- Iterate with the user and update the business spec when the design changes the behavior.
- Put the board link, the artifact links and a short text list of the screens and states into the "Design" section
  of the spec, so that an agent that cannot open the artifact can still work.

## 3. Plan

- Ask with AskUserQuestion: "Do you want to take part in the planning?"
  - Yes: explore and settle the open decisions on the board with `phase: tech`. Then call
    EnterPlanMode and get approval with ExitPlanMode. Plan mode blocks the board, so a decision that
    comes up in plan mode goes in the plan as `Open: <decision>`. Settle it after ExitPlanMode.
  - No: plan without plan mode. For each open decision, take the recommended option, add it to
    `assumed/` on the board and write it in the plan as `Assumed: <decision>`. Only the decisions of
    step 4 go to the user.
- Explore with CodeGraph and the code-conventions references. Read the records in `docs/decisions/`
  that concern the feature.
- Write the plan with the sections of the plan template, also when plan mode is off.
- Never write the plan inside the repository and never commit it. Keep it in the plan file of plan
  mode or in the scratchpad directory.
- Choose the gates from the catalog in the template and apply the AGENTS.md verification levels.
  `integrity` is always mandatory. Added or changed UI makes `unit`, `e2e-desktop`, `e2e-android`
  and `e2e-ios` mandatory.
- If the plan meets a trigger of step 4, do step 4 before the plan is final.

## 4. Technical design (optional)

- Triggers: a new library or Gradle plugin, a new architecture approach or a deviation from the
  architecture reference, a new module, a schema change or a migration, a security or crypto
  change, a root navigation or DI graph change.
- Always do this step with the user, also when the user does not take part in the planning. Settle
  each decision on the board with `phase: tech`, with diagrams and code snippets of the options.
- Record each decision as an ADR in `docs/decisions/NNNN-<title>.md` with the next free number, and
  the feature design in `<feature dir>/tech-design.md`, from the templates. In plan mode, draft
  them in the plan file and write them after ExitPlanMode.
- Go back to step 3 with the decisions.

## 5. Hand-off

1. Run `python3 scripts/feature-factory/gates.py preflight --demo <platforms> <gates>`. On
   `env-error`, tell the user what to fix and continue only when the user confirms.
2. If the session is in manual permission mode, tell the user that the agents can stop on
   permission prompts, and recommend auto mode.
3. Commit the specs and the ADRs, not the plan, with the message `Specify <feature>`. `baseSha` is this commit.
4. Call the Workflow tool with `name: "feature-build"` and the `args` from the template. Pass the
   full plan text.
5. Tell the user that no more input is necessary, that `/workflows` shows the progress, that the run
   uses and stops the emulator and the simulator, and that nobody may change the worktree until the
   run ends.

## 6. Completion

When the workflow notification arrives:

- `passed`: report the branch, the head commit, the gate results, the review nits, the `Assumed:`
  decisions of the plan and the demo path.
- `failed`: report the reason and the detail, and how to continue. Fix the environment for
  `environment`. Make a new plan for `blocked` or `stalled`. For the other reasons, run
  `/feature-build` again with the same `args`, or relaunch the run with `resumeFromRunId` in the same
  session.
- `error`: the `args` are invalid. Correct them and start the workflow again.
- Make sure that `git status` is clean. Keep the branch and do not merge it.
