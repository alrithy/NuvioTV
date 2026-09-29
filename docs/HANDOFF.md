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
G2 IN_PROGRESS. G2a (tier core + fan-out/image/post-play policies, 262–265 and 269–273,
D042, IMPORT_LEDGER G2a) is on `feat/adaptive-resource-manager`. Audit:
`docs/audits/G2_RESOURCE_AUDIT.md`. Local evidence: JVM harness for `fork/` (54 tests, 0 failures,
13 new in AdaptiveResourcesTest); official call sites are compiled and tested only by CI.

## Exact next action
1. Drive the G2a PR to green (full-suite guard, fullDebug) and squash-merge it.
2. G2b on the same branch: playback allocation safety (266–268). `MemoryBudget` tier becomes
   heap-low OR `AdaptiveResources.policy.isConstrained` (only adds safety), Lite's 250 MB buffer
   ceiling and perf-mode parallel clamp on constrained only; correct the three stale
   `NuvioExoPlayerPerformanceHelperTest` expectations to official's intentional values (official
   `2a22a6f22`, native tiers lowered) and remove those debt entries per BASELINE_TEST_DEBT.
3. G2c: bounded metadata/rating caches (LruCacheMap), offline queue, stream-list recomposition
   (Lite c760295eb), bidi (d491bd09e), MPV low-RAM cache; 279 (Seekr) → defer to G7.
4. Upstream observation: official `7d3cea0` (#3740, audio delay UI) is not accepted and touches
   no G2 owner; sync at a later gate boundary.

## Evidence / limits
Local Android execution is unavailable (no Android SDK); use GitHub CI evidence. Actions artifacts
cannot be downloaded here (blob storage egress blocked); the baseline-audit workflow prints the
inventory to the job log. No hardware/manual tests run or claimed. MANUAL-PENDING from G1: TV focus
of the add-on badge; on-device HUD values (refresh, passthrough, DV); D-pad focus/apply/revert on
the device assessment card. MANUAL-PENDING from G2a: memory/scrolling behavior on 1–2 GB boxes.

## Ownership / unfinished work
Sequential writer, no lease. Admin actions (default branch, protection) remain in
GITHUB_ADMIN_CHECKLIST; connector access to those settings is not available.
