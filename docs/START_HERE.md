# START HERE — Nuvio Superfork

You should be able to understand this project without any previous chat.

## What is this?
A maintainable "best of Nuvio forks" integration project built on current official NuvioTV. Proven fork implementations are reused/ported instead of casually rewritten.

## What do I do right now?
1. Read integration/state.yaml.
2. Read docs/PROJECT_STATUS.md.
3. Read docs/HANDOFF.md.
4. Follow docs/AGENT_PLAYBOOK.md.
5. Work only on active_branch and active_gate.
6. Use docs/GATE_SPECS.md for exact deliverables/exit criteria.
7. Use integration/feature_traceability.csv for the exact feature IDs, source group, owner and status.

## Complete scope
docs/MASTER_FEATURES.md contains exactly 320 stable feature IDs. Do not silently omit one.

## Where code comes from
- docs/SOURCE_MAP.md: pinned repositories/branches/SHAs.
- docs/FORK_RESEARCH.md: audited commits/behavior/caveats.
- docs/DECISIONS.md: architecture decisions and exclusions.
- docs/IMPORT_LEDGER.md: what was actually imported.

Default strategy:
REUSE -> CHERRY-PICK -> FILE_PORT -> DELTA_PORT -> ALGORITHM_PORT -> ADAPTER -> REWRITE LAST.

Never merge a whole fork.

## Branches
- dev: legacy/default branch; do not develop Superfork features there.
- superfork/integration: integration target; no direct feature development.
- integration/state.yaml active_branch: the branch to work on now.

Exactly one active writer/agent per task branch.

## Completion
A gate is complete only under docs/DEFINITION_OF_DONE.md.
Hardware/manual tests must be recorded as PASS / FAIL / MANUAL-PENDING; never guessed.

## Problems
- Dirty/conflicted/unknown work: docs/FAILURE_RECOVERY.md.
- Official upstream changed: docs/UPSTREAM_SYNC.md.
- Secrets/security: docs/SECURITY_POLICY.md.
- Release readiness: docs/RELEASE_POLICY.md.

## If the user says only "اشتغل على نوفيو"
Do not ask them to repeat the brief. Run preflight, read state/handoff, and continue the active gate. Ask only if repository evidence cannot resolve a real blocker.
