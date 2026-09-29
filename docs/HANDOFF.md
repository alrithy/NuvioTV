# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G2 — Adaptive Resource Manager
- Active branch: `feat/adaptive-resource-manager`
- Status: IN_PROGRESS
- Task: `tasks/G2_ADAPTIVE_RESOURCE_MANAGER.md`
- Accepted official baseline: `71632b9271e8bce6783e415d64f34cfa4e8b894c`
- Governance: READY
- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.
<!-- canonical-state:end -->

## Current work boundary
G0 DONE (PR #7). Official `71632b9` accepted (PR #9, D040).
G1 DONE: G1a add-on health (PR #10, `d8511d8`), G1b diagnostics HUD (PR #11, `70c99a2`), G1c
device assessment (PR #12, `27c921c`). 289 deferred to G8 (D041). Audit:
`docs/audits/G1_DIAGNOSTICS_AUDIT.md`; provenance: IMPORT_LEDGER G1a/G1b/G1c.
G2 IN_PROGRESS. Merged: G2a (PR #14, `b2a13d1`, 262–265/269–273) and G2b (PR #15, `9002d54`,
266–268; debt 19 → 16 after runs 36564393631 + confirming 36565841444: 1816 tests, 15 known
failures, 0 new). G2c (274, 275, 277, 278; 276 deferred, 279 → G7, 280 → G3; IMPORT_LEDGER G2c)
is on `feat/adaptive-resource-manager`. Audit: `docs/audits/G2_RESOURCE_AUDIT.md`. Local evidence:
JVM harness for `fork/` (62 tests, 0 failures); official call sites only via CI.

## Exact next action
1. Drive the G2c PR to green (full-suite guard, fullDebug) and squash-merge it.
2. G2 closeout PR (like #13): G2 DONE in `completed_gates` (PRs 14, 15, G2c; deferred
   [276, 279, 280]), activate G3 Playback Strategy Framework (`feat/playback-strategies`,
   `tasks/G3_*`), refresh HANDOFF/PROJECT_STATUS.
3. Upstream observation: official `7d3cea0` (#3740, audio delay UI) is not accepted and touches
   no G2 owner; check official again at the G2→G3 boundary and sync before G3 if it touches
   player strategy owners.

## Evidence / limits
Local Android execution is unavailable (no Android SDK); use GitHub CI evidence. Actions artifacts
cannot be downloaded here (blob storage egress blocked); the baseline-audit workflow prints the
inventory to the job log. No hardware/manual tests run or claimed. MANUAL-PENDING from G1: TV focus
of the add-on badge; on-device HUD values (refresh, passthrough, DV); D-pad focus/apply/revert on
the device assessment card. MANUAL-PENDING from G2: memory/scrolling (G2a) and startup/seek with the lower budget (G2b) on
1–2 GB boxes; source-panel focus smoothness and RTL rendering (G2c).

## Ownership / unfinished work
Sequential writer, no lease. Admin actions (default branch, protection) remain in
GITHUB_ADMIN_CHECKLIST; connector access to those settings is not available.
