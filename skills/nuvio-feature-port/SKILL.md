---
name: nuvio-feature-port
description: "Use for importing, comparing or porting a Nuvio Superfork feature. Read current official upstream, live SOURCE_MAP pins and feature_traceability before selecting the smallest missing delta."
---

# Port one verified behavior

Start with the repository preflight and active task from `nuvio-orchestrator`.
In this session read working-repo AGENTS, state, `docs/HANDOFF.md` (AGENT_HANDOFF),
affected `integration/feature_traceability.csv` rows, `docs/MASTER_FEATURES.md`
IDs, `docs/SOURCE_MAP.md`, `docs/PORTING_PROTOCOL.md`, `docs/FORK_RESEARCH.md`,
`docs/IMPORT_LEDGER.md` and applicable gate/DoD/upstream/license policies.

Check official upstream's **current** branch HEAD and relevant files/commits
before every port. Distinguish the observed official HEAD from the repository's
accepted baseline. If moved seams require synchronization, follow `docs/UPSTREAM_SYNC.md`
through a separate sync PR. Never silently change a source pin.

Resolve traceability source aliases through SOURCE_MAP. Inspect code and tests at
the full pinned SHA; verify the harvested commits belong to that source history.
Compare both accepted and relevant current official behavior. Classify equivalent,
missing delta, isolated algorithm, obsolete/reverted or blocked with evidence.
Use the live PORTING_PROTOCOL preference order; reuse official behavior where
equivalent. Preserve current architecture owners and official fallback. Never
merge a whole fork or overwrite a newer official file with an older source copy.

Record source repo/branch/full SHA/commits/files, comparison, intended delta,
adaptations, licensing and tests in IMPORT_LEDGER before importing. Update only
affected traceability rows. Port relevant tests with the smallest correct change,
run the live automated requirements and record device-only tests as MANUAL-PENDING
according to the current policy. Finish with `nuvio-handoff`, then return to the
orchestrator's next unit; do not stop solely for manual TV testing.
