# Nuvio Superfork — Agent Instructions

These instructions apply to Codex, Claude Code, Gemini, Copilot, or any coding agent.

## Zero-prompt behavior
If the user only says "اشتغل على نوفيو", "work on Nuvio", or "continue Nuvio", do not ask for the project brief. The repository is the brief.

### Mandatory preflight
1. Inspect:
   ```bash
   git status --short
   git branch --show-current
   git rev-parse HEAD
   ```
2. Run:
   ```bash
   python3 scripts/superfork/validate_project_state.py
   ```
3. Read:
   - integration/state.yaml
   - docs/PROJECT_STATUS.md
   - docs/HANDOFF.md
   - docs/START_HERE.md
   - docs/AGENT_PLAYBOOK.md
   - docs/GATE_SPECS.md
   - docs/DEFINITION_OF_DONE.md
   - docs/BASELINE.md
   - docs/ARCHITECTURE.md
   - docs/MASTER_FEATURES.md
   - docs/DECISIONS.md
   - docs/FORK_RESEARCH.md
   - docs/SOURCE_MAP.md
   - docs/ROADMAP.md
   - docs/GATE_CHECKLISTS.md
   - docs/DEFINITION_OF_DONE.md
   - docs/TEST_MATRIX.md
   - docs/IMPORT_LEDGER.md
   - docs/RUNBOOK.md
   - docs/BRANCHING.md
   - docs/RECOVERY.md
   - docs/UPSTREAM_SYNC.md
   - integration/features.yaml
   - integration/feature_coverage.yaml
   - integration/state.yaml
   - integration/feature_traceability.csv
4. Continue the active gate/task from `integration/state.yaml` and `docs/HANDOFF.md`.

Only ask the user if a true blocker remains after inspecting Git and these documents.

## Branch safety
- `dev` is legacy/default until GitHub admin changes the default. Never implement Superfork features there.
- Never code directly on `superfork/integration`.
- Work on `integration/state.yaml: active_branch` or a documented task branch.
- Never run two agents concurrently on the same task branch.
- Preserve any existing uncommitted work; understand it before editing.

## Authority order
1. current explicit user instruction
2. this file
3. docs/DECISIONS.md
4. docs/GATE_SPECS.md / docs/ROADMAP.md
5. docs/MASTER_FEATURES.md
6. docs/FORK_RESEARCH.md / docs/SOURCE_MAP.md
7. integration/state.yaml / docs/HANDOFF.md for progress

## Core integration strategy
REUSE -> CHERRY-PICK -> FILE_PORT -> DELTA_PORT -> ALGORITHM_PORT -> ADAPTER -> REWRITE ONLY AS LAST RESORT.

Never merge another fork wholesale. Never replace a current official subsystem with an older copy without a file-level diff. If official already has equivalent behavior, keep official and port only the missing delta.

## Non-negotiable architecture
- one subtitle engine
- one diagnostics model/UI owner
- one skip aggregator
- one adaptive resource manager
- one stream ranker
- official playback remains fallback/selectable for core paths
- AI and MAT remain experimental/OFF by default
- no stable Smart Vibrance import
- no inheritance of ysosrs feature removals
- no obsolete parallel self-host stack when official already owns it

## Source provenance
- Use pinned SHAs in SOURCE_MAP.
- Do not silently update a source pin.
- Record source commits/files and license notes in IMPORT_LEDGER.
- Do not drop a difficult feature; mark blocked/deferred with reason.

## Quality
Every code gate must satisfy `docs/DEFINITION_OF_DONE.md`.
At minimum run the governance validator and relevant Gradle tests/build. Never claim hardware/manual tests passed when they were not run.

## Handoff
Before stopping:
- commit coherent work;
- update IMPORT_LEDGER if code was imported;
- update integration/state.yaml;
- update docs/HANDOFF.md;
- run validator;
- leave a clean tree or document why not;
- report exact next action.

## Current project state
Do not hardcode progress here. Read `integration/state.yaml`.