---
name: nuvio-handoff
description: "Use after each Nuvio work unit or before interruption/agent change. Update live feature_traceability and the existing AGENT_HANDOFF (docs/HANDOFF.md) so a fresh agent can continue without conversation history."
---

# Persist a restartable work boundary

Read working-repo AGENTS, `docs/GOVERNANCE_OWNERS.md`, state/task, current
`docs/HANDOFF.md` (AGENT_HANDOFF), affected feature_traceability rows,
MASTER_FEATURES IDs, SOURCE_MAP entries and the live AGENT_PLAYBOOK/DoD/status
semantics. Do not create a parallel AGENT_HANDOFF or snapshot project policy
inside the plugin. GitHub versioned repository files own durable knowledge.

After every coherent unit:

- Update affected `integration/feature_traceability.csv` rows with actual status,
  evidence, reason and next action. Governance/plugin work has no application
  feature IDs: record N/A rather than changing unrelated rows or gate progress.
- Update `integration/state.yaml` only for actual task/progress changes. Keep
  accepted pins and last-green evidence honest. Never call unmerged or untested
  work DONE. Update IMPORT_LEDGER only when source behavior/code was imported.
- Update `docs/HANDOFF.md` and `docs/PROJECT_STATUS.md` with scope, branch,
  verified ancestor SHA, changed paths/IDs, commands/results, exact-head CI URLs,
  blockers, manual pending checks, and exact next action. Regenerate owned
  headers with `python3 scripts/superfork/state_view.py`; do not hand-edit them.
  Do not invent a self-referential commit SHA or hardware PASS.
- Validate with the existing governance validator, relevant tests and
  `git diff --check`; commit only scoped coherent files. Push normally if within
  the session's authorization and verify the remote HEAD. Preserve/document
  interrupted dirty work; never hide it. Release only a lease actually owned.

Leave enough evidence for a new agent to distinguish local implementation, review,
merge and manual certification. Then return to the orchestrator and continue the
next authorized unit. At gate completion follow the current transition procedure
and task queue; advance only with merged code and required automated evidence.
