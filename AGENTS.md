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

Then:
1. Fetch/read **remote** `superfork/integration:integration/state.yaml`; do not trust stale local state.
2. Switch to the recorded `active_branch` only after preserving unknown work.
3. On that branch run `python3 scripts/superfork/preflight.py --fetch`.
4. Read only the minimum execution set:
   - `integration/state.yaml`
   - `docs/HANDOFF.md`
   - the active task packet from `active_task_packet`
   - the active gate section in `docs/GATE_SPECS.md`
   - `docs/DEFINITION_OF_DONE.md`
5. Use the optional writer lease only if two coding sessions might overlap. Sequential agent handoffs do not require a lease.

Read additional documents **only when the active task needs them**:
- importing/porting code: `SOURCE_MAP`, `FORK_RESEARCH`, `PORTING_PROTOCOL`, `IMPORT_LEDGER`
- architecture trade-off: `DECISIONS`, `COMPONENT_MAP`, `PRODUCT_REQUIREMENTS`, `NON_GOALS`
- testing/completion: `TEST_STRATEGY`, `TEST_MATRIX`, `BASELINE_TEST_DEBT`
- security/dependencies/data/upstream/release: the corresponding policy document
- feature status: only the affected rows in `integration/feature_traceability.csv`

Do **not** read the whole 320-feature inventory or every policy document on every session unless the task genuinely requires it. Prefer repository validators and task packets over manual ceremony.

Only ask the user if a genuine blocker remains after repository inspection.

See `docs/GOVERNANCE_OWNERS.md` when ownership/canonical-source ambiguity exists.
Superfork contributions follow `CONTRIBUTING_SUPERFORK.md`.

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
- Exactly one active writer per task branch by policy. The optional lease in AGENT_PLAYBOOK is only for intentional/possible overlap; normal sequential work relies on fresh remote-head checks and Git non-fast-forward protection.
- Preserve and inspect unknown uncommitted work before editing.
- Never force-push task/integration history. If an optional `agent-locks/` lease is used, only compare-and-delete your exact lease on release.

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

## Manual hardware validation
For G1–G13, do not pause development solely because a device-only check is MANUAL-PENDING. Record the exact check/evidence needed and continue once code is merged and automated CI/DoD is green. The project intentionally batches all remaining hardware/manual checks into one consolidated G14 certification campaign. Never invent PASS results, and never ship Stable while a release-blocking MANUAL-PENDING item remains.

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