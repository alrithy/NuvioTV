---
name: nuvio-ci-recovery
description: "Use for Nuvio Superfork failing CI, stale branches, dirty trees or interrupted work. Diagnose exact-head evidence and follow RECOVERY.md and its canonical target without discarding unknown work."
---

# Recover from evidence

Read current Git identity/status/branch/HEAD/remotes and working-repo AGENTS,
state, `docs/HANDOFF.md` (AGENT_HANDOFF), affected feature_traceability and
MASTER_FEATURES scope, SOURCE_MAP entries, `docs/RECOVERY.md` and its target
`docs/FAILURE_RECOVERY.md`. Do not reproduce or invent a second recovery policy.

For dirty work, inspect staged/unstaged/untracked content and ownership; preserve
coherent checkpoints excluding secrets/build outputs. For old/wrong branches,
read freshly fetched remote integration state, inspect unique commits and use
the existing recovery procedure. Do not blindly reset, clean, stash, switch or
force-push. Conflicts require file-level reasoning and documented resolutions.

For CI, inspect the run's SHA, job/step logs and reports, not just a badge. Check
`docs/BASELINE_TEST_DEBT.md`, the canonical debt registry, live DoD/TEST_MATRIX
and workflow definitions. Reproduce with `scripts/superfork/run_full_unit_suite.py`
and the relevant build/test; classify infrastructure, incomplete/missing reports,
new failures, new skips, or registered debt using the existing guard. Never
automatically expand baseline debt or claim PASS for an incomplete run.

Apply the smallest justified repair and rerun the failed gate; a rerun is justified
only by evidence of a transient failure. Verify required checks on the resulting
exact HEAD. Missing execution access is an explicit limitation; seek GitHub CI
evidence before claiming completion. Manual-only TV checks remain pending under
the live development policy, while actual automated failures remain blockers.

Update handoff/affected traceability with cause, change, commands, results, URLs
and next action using `nuvio-handoff`. Resume authorized work once the blocker is
resolved; ask only for a decision that repository evidence cannot safely settle.
