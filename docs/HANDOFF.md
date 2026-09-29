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
G2 IN_PROGRESS. G2a (tiers + fan-out/image/post-play, 262–265 and 269–273) merged: PR #14,
`b2a13d1`, exact-head run 36562615830 (1814 tests, 0 new failures, fullDebug built).
G2b (playback allocation safety, 266–268, IMPORT_LEDGER G2b) is on
`feat/adaptive-resource-manager`: constrained devices join `MemoryBudget`'s low tier, 250 MB heap
budget ceiling, 4 session connections; stale `NuvioExoPlayerPerformanceHelperTest` expectations
corrected to official `2a22a6f22`. Audit: `docs/audits/G2_RESOURCE_AUDIT.md`. Local evidence:
JVM harness for `fork/` (56 tests, 0 failures); official call sites only via CI.

## Exact next action
1. G2b PR #15: run 36564393631 (head `9debaab`) passed the three corrected
   `NuvioExoPlayerPerformanceHelperTest` cases (1816 tests, 15 failures, 0 new). Their debt
   entries are removed (19 → 16, BASELINE_TEST_DEBT.md "G2b removals"); when the removal push's
   exact-head run is green (confirming run), squash-merge.
2. G2c: bounded metadata/rating caches (LruCacheMap), offline queue, stream-list recomposition
   (Lite c760295eb), bidi (d491bd09e), MPV low-RAM cache; 279 (Seekr) → defer to G7.
3. Upstream observation: official `7d3cea0` (#3740, audio delay UI) is not accepted and touches
   no G2 owner; sync at a later gate boundary.

## Evidence / limits
Local Android execution is unavailable (no Android SDK); use GitHub CI evidence. Actions artifacts
cannot be downloaded here (blob storage egress blocked); the baseline-audit workflow prints the
inventory to the job log. No hardware/manual tests run or claimed. MANUAL-PENDING from G1: TV focus
of the add-on badge; on-device HUD values (refresh, passthrough, DV); D-pad focus/apply/revert on
the device assessment card. MANUAL-PENDING from G2: memory/scrolling (G2a) and startup/seek with the lower budget (G2b) on
1–2 GB boxes.

## Ownership / unfinished work
Sequential writer, no lease. Admin actions (default branch, protection) remain in
GITHUB_ADMIN_CHECKLIST; connector access to those settings is not available.
