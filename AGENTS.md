# Nuvio Superfork — Agent Instructions

These instructions apply to Codex, Claude Code, Gemini, Copilot, or any coding agent.

## Zero-prompt behavior
If the user only says "اشتغل على نوفيو", "work on Nuvio", or "continue Nuvio", do not ask for the project brief. The repository is the brief.

## Mandatory preflight
1. Run:
   ```bash
   git status --short
   git branch --show-current
   git rev-parse HEAD
   python3 scripts/superfork/validate_project_state.py
   ```
2. Read every session:
   - integration/state.yaml
   - CONTRIBUTING_SUPERFORK.md
   - docs/PROJECT_STATUS.md
   - docs/HANDOFF.md
   - docs/AGENT_PLAYBOOK.md
   - docs/GATE_SPECS.md for the active gate
   - docs/DEFINITION_OF_DONE.md
   - docs/DECISIONS.md
3. Read as relevant to the active task:
   - docs/MASTER_FEATURES.md + integration/feature_traceability.csv
   - docs/ARCHITECTURE.md + docs/OWNERSHIP_MAP.md
   - docs/FORK_RESEARCH.md + docs/SOURCE_MAP.md + docs/IMPORT_LEDGER.md
   - docs/TEST_MATRIX.md + docs/PERFORMANCE_VALIDATION.md + docs/MANUAL_TEST_LOG.md
   - docs/FEATURE_FLAG_POLICY.md
   - docs/DATA_MIGRATIONS.md
   - docs/SECURITY_POLICY.md
   - docs/LICENSE_AND_ATTRIBUTION.md
   - docs/RISK_REGISTER.md
4. Continue the active gate/task from state + HANDOFF.

Only ask the user if a true blocker remains after Git and repository evidence are exhausted.

## Contribution policy split
- Superfork branches: `CONTRIBUTING_SUPERFORK.md`.
- Legacy `dev`: upstream `CONTRIBUTING.md`.
- Upstream's temporary no-feature policy does not block the documented Superfork roadmap.

## Branch safety
- Never implement Superfork features on `dev`.
- Never code directly on `superfork/integration`.
- Use the documented active/task branch.
- Never run two agents concurrently on the same task branch.
- Preserve unknown uncommitted work before editing/resetting.

## Authority order
1. current explicit user instruction
2. AGENTS.md
3. CONTRIBUTING_SUPERFORK.md
4. docs/DECISIONS.md
5. docs/GATE_SPECS.md / ROADMAP
6. docs/MASTER_FEATURES.md
7. source/research docs
8. state/HANDOFF for progress

## Core strategy
REUSE -> CHERRY-PICK -> FILE_PORT -> DELTA_PORT -> ALGORITHM_PORT -> ADAPTER -> REWRITE ONLY AS LAST RESORT.

Never merge a whole fork. Diff current official first. Import only missing delta. Preserve provenance.

## Architectural invariants
Follow `docs/OWNERSHIP_MAP.md`: one owner per concern. Official playback remains fallback/selectable for core paths. AI/MAT are experimental OFF by default. Do not import reverted Smart Vibrance as stable, inherit ysosrs removals, or recreate obsolete self-host infrastructure already official.

## Quality and honesty
Satisfy DoD. Run governance + relevant Gradle/gate checks. Use baseline-test-debt controls. Never claim hardware/manual tests passed without a real run.

## Handoff
Commit coherent work; update IMPORT_LEDGER/state/HANDOFF; rerun validator; leave a clean tree or document why not; state the exact next action.

Do not hardcode current progress here. Read `integration/state.yaml`.
