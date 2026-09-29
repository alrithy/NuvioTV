# Canonical ownership

Each concern has one owner. Summaries and task packets link/refine it; they cannot override it.

| Concern | Canonical owner | Consumers / compatibility |
|---|---|---|
| Current gate, branch, status, ownership | `integration/state.yaml` | Generated headers in PROJECT_STATUS/HANDOFF; preflight checks remote state |
| Gate order, dependency, task branch/packet | `integration/task_queue.csv` | ROADMAP narrative, tasks/ execution detail; no independent progress field |
| Gate specification | `docs/GATE_SPECS.md` | GATE_CHECKLISTS redirect; tasks refine scope |
| Completion contract | `docs/DEFINITION_OF_DONE.md` | STATUS_MODEL defines vocabulary |
| Agent procedure | `docs/AGENT_PLAYBOOK.md` | AGENTS entrypoint; RUNBOOK redirect |
| Recovery | `docs/FAILURE_RECOVERY.md` | RECOVERY redirect |
| Governance validation | `scripts/superfork/validate_project_state.py` | superfork_guard.py delegates; no second validator |
| Full-suite failure classification | `scripts/superfork/check_baseline_test_failures.py` | run_full_unit_suite.py executes; check_test_failures.py redirects |
| Known test debt | `integration/baseline_test_debt.json` | BASELINE_TEST_DEBT explains policy; old TXT files are redirects |
| Stable feature scope | `docs/MASTER_FEATURES.md` | IDs 1–320 never renumbered |
| Feature traceability/progress | `integration/feature_traceability.csv` | features.yaml is a compatibility pointer |
| Source provenance (planned) | `docs/SOURCE_MAP.md` | FORK_RESEARCH contains findings, not a second pin registry |
| Source provenance (imported) | `docs/IMPORT_LEDGER.md` | includes exact source/delta/tests/license/resulting local commit |
| Architecture decisions | `docs/DECISIONS.md` | ARCHITECTURE overview; COMPONENT_MAP maps integration seams |
| Component ownership | `docs/COMPONENT_MAP.md` | OWNERSHIP_MAP redirect |
| Test policy / cases / execution evidence | `docs/TEST_STRATEGY.md` / `docs/TEST_MATRIX.md` / `docs/MANUAL_TEST_LOG.md` | PERFORMANCE_VALIDATION redirect |
| Data migrations | `docs/DATA_MIGRATION_POLICY.md` | DATA_MIGRATIONS redirect |
| Feature flag lifecycle | `docs/FEATURE_FLAG_POLICY.md` | G0 packet defines initial API scope |
| Licensing | `docs/LICENSE_AND_ATTRIBUTION.md` | IMPORT_LEDGER records each import |
| Release contract | `docs/RELEASE_POLICY.md` | RELEASE_CHECKLIST redirects |

## Supersession audit
PR #5 head `b6612250d9fa65d12b05918e94682928a1f2da34` was reviewed file by file.
Preserved: contribution-policy split, flag lifecycle, hardware log template, license policy,
performance metrics/resource tiers (TEST_STRATEGY/TEST_MATRIX), data migration constraints,
component ownership, risks, full-suite debt intent and workflow timeouts.
Replaced: its old c257 baseline/READY state, malformed upstream PR-policy YAML, simple-name
allowlist and count-locked validator. PR #6 is the sole governance integration path.
The #5 branch and commits remain available for history; PR #5 was closed as superseded after #6 merged.
Legacy PR #1/#2 remain reference-only under LEGACY_WORK.