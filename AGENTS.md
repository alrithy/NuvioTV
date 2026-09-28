# Nuvio Superfork — Agent Instructions

You are working on the Nuvio Superfork. These rules are agent-agnostic and apply to Codex, Claude Code, Gemini, Copilot, or any other coding agent.

## Zero-prompt mode

If the user says only something like:
- "اشتغل على نوفيو"
- "work on Nuvio"
- "continue Nuvio"

do not ask what to do next.

Instead:
1. Inspect the current branch, HEAD, working tree, open task context, and recent commits.
2. Read:
   - docs/HANDOFF.md
   - docs/BASELINE.md
   - docs/ARCHITECTURE.md
   - docs/SOURCE_MAP.md
   - docs/ROADMAP.md
   - docs/TEST_MATRIX.md
   - integration/features.yaml
3. Continue the next incomplete roadmap gate recorded in docs/HANDOFF.md.
4. If the current branch is a task branch with unfinished work, continue that work first.
5. If the working tree contains uncommitted work from another agent, preserve it and understand it before editing.
6. Only ask the user a question if a real blocking ambiguity cannot be resolved from Git/repository state.

Git and repository documentation are the source of truth. Never depend on previous chat history.

## Baseline

Official upstream:
NuvioMedia/NuvioTV:dev@c257a2365ee3386b582dc2974ec235cfe0381f33

Integration branch:
superfork/integration

Never work directly on superfork/integration. Use task branches and PRs.

## Core strategy

Do not rewrite proven fork features by default.

Preferred order:
REUSE -> CHERRY-PICK -> FILE_PORT -> DELTA_PORT -> ALGORITHM_PORT -> ADAPTER -> REWRITE ONLY AS LAST RESORT.

## Rules

1. Never merge another fork wholesale.
2. Never replace a current official subsystem with an older fork copy without a file-level diff.
3. If upstream already contains equivalent functionality, keep upstream and port only the missing delta.
4. Preserve license notices and record source repository + branch + SHA/commits for every imported feature.
5. One owner per concern:
   - one subtitle engine
   - one diagnostics model
   - one skip aggregator
   - one adaptive resource manager
   - one stream ranker
6. Keep official playback available as selectable/fallback behavior.
7. AI and MAT remain experimental and OFF by default.
8. Every gate/feature must build and test before the next begins.
9. Do not discard, reset, force-push, or overwrite work you did not create unless explicitly instructed.
10. Keep changes scoped to the current roadmap gate.

## Workflow for every imported feature

- Audit source repo/branch/SHA/commits and source files.
- Identify equivalent files in the pinned/current official baseline.
- Record what already exists upstream.
- Import the smallest coherent unit.
- Adapt only boundaries required by current APIs.
- Add a feature flag/fallback for core behavior where practical.
- Port/add tests.
- Update docs/IMPORT_LEDGER.md.
- Update docs/HANDOFF.md.
- Stop if the current gate is not green.

## Handoff requirement

Before finishing any work session:
1. Run relevant build/tests.
2. Commit completed work with a clear conventional commit.
3. Update docs/HANDOFF.md with:
   - current branch
   - latest commit SHA
   - gate/task completed
   - files/modules changed
   - tests/build commands and results
   - unresolved issues
   - exact next recommended action
4. Leave the repository in a state another agent can continue without conversation history.

## Current starting task

Gate 0 — Fork Foundation.

Tasks:
1. Verify HEAD derives from the pinned baseline.
2. Create com.nuvio.tv.fork.foundation.
3. Add FeatureId and FeatureMode (OFF, ON, AUTO).
4. Add FeatureRegistry with defaults preserving official behavior.
5. Add ForkSettingsDataStore only if actually required; do not migrate official settings.
6. Add SourceAttribution model/utility.
7. Add docs/IMPORT_LEDGER.md template.
8. Add unit tests for feature default semantics.
9. Run build/tests.
10. Commit as: chore(fork): establish integration foundation

Gate 0 must not change user-visible playback or UI behavior.

After Gate 0, continue with Gate 1 — Unified Diagnostics as defined in docs/ROADMAP.md.
