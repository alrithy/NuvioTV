# Nuvio Superfork — Agent Instructions

These instructions apply to Codex, Claude Code, Gemini, Copilot, or any coding agent.

## Zero-prompt behavior
If the user only says "اشتغل على نوفيو", "work on Nuvio", or "continue Nuvio", do not ask for the project brief. The repository is the brief.

## Mandatory preflight
Before editing code:
```bash
git status --short
git branch --show-current
git rev-parse HEAD
python3 scripts/superfork/validate_project_state.py
```

Then fetch and read **remote integration state** before selecting a branch. On the task
branch run `python3 scripts/superfork/preflight.py --fetch`. Read AGENT_PLAYBOOK for
the atomic writer lease and recovery; do not edit until the branch is fresh and claimed.
Install validator dependencies from `scripts/superfork/requirements.txt` if needed.

Then read, in this order:
1. `integration/state.yaml` — machine-readable active gate/branch/status/task packet.
2. `docs/PROJECT_STATUS.md` — one-page human status.
3. `docs/HANDOFF.md` — exact continuation notes.
4. `docs/AGENT_PLAYBOOK.md` — execution procedure.
5. Read the active task packet declared in `integration/state.yaml`, then `docs/GATE_SPECS.md` for gate scope/exit criteria.
6. `integration/feature_traceability.csv` — authoritative mapping of all 320 feature IDs.
7. `docs/DEFINITION_OF_DONE.md`.
8. `docs/DECISIONS.md`.
9. Relevant sections of `docs/FORK_RESEARCH.md`, `docs/SOURCE_MAP.md`, `docs/COMPONENT_MAP.md`, `docs/PORTING_PROTOCOL.md`, and `docs/TEST_MATRIX.md`.
10. `docs/PRODUCT_REQUIREMENTS.md` and `docs/NON_GOALS.md` before making a trade-off or changing scope.
11. `docs/STATUS_MODEL.md`, `docs/TEST_STRATEGY.md`, and `docs/BASELINE_TEST_DEBT.md` before declaring work complete.
12. `docs/SECURITY_POLICY.md`, `docs/DEPENDENCY_POLICY.md`, `docs/DATA_MIGRATION_POLICY.md`, `docs/UPSTREAM_SYNC.md`, and `docs/RELEASE_POLICY.md` when applicable.

Only ask the user if a genuine blocker remains after repository inspection.

See `docs/GOVERNANCE_OWNERS.md` for the complete one-owner map and supersession audit.
Superfork contributions follow `CONTRIBUTING_SUPERFORK.md`; upstream CONTRIBUTING
restrictions govern legacy dev, not the approved Superfork roadmap.

## Canonical sources of truth
- Current progress/branch/status/task packet: `integration/state.yaml`
- Gate execution queue: `integration/task_queue.csv` + `tasks/`
- Human continuation context: `docs/HANDOFF.md`
- Full requested scope: `docs/MASTER_FEATURES.md` (IDs 1–320)
- Per-feature gate/source/owner/status: `integration/feature_traceability.csv`
- Gate sequencing: `docs/ROADMAP.md`
- Gate scope/entry/exit: `docs/GATE_SPECS.md`
- Architecture/product decisions: `docs/DECISIONS.md`
- Pinned external sources: `docs/SOURCE_MAP.md`
- Audited fork findings: `docs/FORK_RESEARCH.md`
- Imported-code provenance: `docs/IMPORT_LEDGER.md`
- Definition of completion: `docs/DEFINITION_OF_DONE.md`
- Execution procedure: `docs/AGENT_PLAYBOOK.md`
- Porting procedure: `docs/PORTING_PROTOCOL.md`
- Component ownership/seams: `docs/COMPONENT_MAP.md`
- Status semantics: `docs/STATUS_MODEL.md`
- Product trade-offs: `docs/PRODUCT_REQUIREMENTS.md`
- Scope boundaries: `docs/NON_GOALS.md`
- Testing model/baseline debt: `docs/TEST_STRATEGY.md` + `docs/BASELINE_TEST_DEBT.md`
- Review/risk controls: `docs/CODE_REVIEW_CHECKLIST.md` + `docs/RISK_REGISTER.md`
- Dependency/data safety: `docs/DEPENDENCY_POLICY.md` + `docs/DATA_MIGRATION_POLICY.md`

Compatibility/redirect documents are not independent sources of truth.

## Authority order
1. Current explicit user instruction.
2. This file.
3. `docs/DECISIONS.md`.
4. `docs/GATE_SPECS.md` and `docs/ROADMAP.md`.
5. `docs/MASTER_FEATURES.md` and `integration/feature_traceability.csv`.
6. `docs/FORK_RESEARCH.md` and `docs/SOURCE_MAP.md`.
7. `integration/state.yaml` and `docs/HANDOFF.md` for progress only.

## Branch safety
- `dev` is legacy/default until GitHub admin changes the default. Never implement Superfork features there.
- Never develop directly on `superfork/integration`.
- Work on `integration/state.yaml: active_branch`.
- Exactly one active writer per task branch; acquire the atomic lease described in AGENT_PLAYBOOK.
- Preserve and inspect unknown uncommitted work before editing.
- Never force-push task/integration history. Only compare-and-delete your exact `agent-locks/` lease on release.

## Core integration strategy
REUSE -> CHERRY-PICK -> FILE_PORT -> DELTA_PORT -> ALGORITHM_PORT -> ADAPTER -> REWRITE ONLY AS LAST RESORT.

Never merge another fork wholesale. Never overwrite a current official subsystem with an older fork copy without a file-level diff. If official already contains equivalent behavior, keep official and port only the missing delta.

## Non-negotiable architecture
- one subtitle engine
- one diagnostics model/UI owner
- one skip aggregator
- one adaptive resource manager
- one stream ranker
- official playback remains fallback/selectable for core paths
- AI and MAT remain experimental/OFF by default
- do not import reverted Smart Vibrance into stable
- do not inherit ysosrs feature removals
- do not re-port obsolete self-host infrastructure already owned by official upstream

## Provenance and completeness
- Use pinned SHAs in `docs/SOURCE_MAP.md`.
- Never silently update a source pin.
- Record source commits/files, adaptations, tests and license notes in `docs/IMPORT_LEDGER.md`.
- Identify affected feature IDs from `integration/feature_traceability.csv`.
- Never silently drop a difficult feature; mark blocked/deferred with evidence.
- Never invent hardware/manual test results.

## Quality gate
Every code gate must satisfy `docs/DEFINITION_OF_DONE.md`.
At minimum run the governance validator, the full unit suite with the no-new-regressions baseline guard, the relevant build, and gate-specific tests. Pre-existing failures are governed only by `docs/BASELINE_TEST_DEBT.md`; new failures must not be waived silently.

## Handoff
Before stopping:
- commit coherent work;
- update IMPORT_LEDGER when external behavior/code was imported;
- update `integration/state.yaml`;
- update `docs/HANDOFF.md` and `docs/PROJECT_STATUS.md`;
- rerun the validator;
- leave a clean tree or document why not;
- record exact next action.

## Current project state
Never hardcode progress here. Read `integration/state.yaml`.