# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G2 — Adaptive Resource Manager
- Active branch: `feat/adaptive-resource-manager`
- Status: READY
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
G2 Adaptive Resource Manager is READY; no G2 code yet.

## Exact next action
1. Create `feat/adaptive-resource-manager` from integration HEAD; run
   `python3 scripts/superfork/preflight.py --fetch`; read `tasks/G2_ADAPTIVE_RESOURCE_MANAGER.md`.
2. Audit official first: `NuvioExoPlayerPerformanceHelper` (RAM detection, tiers, safe/warning
   native limits), `MemoryBudget` (heap tiers, parallel overhead, chunk caps), poster/image cache
   config, addon request concurrency, post-play prefetch, metadata/rating caches, offline queue.
   Classify features 262–280 against hackerslash/NuvioTV-Lite @ 2afdcd05 (SOURCE_MAP); port only
   the missing delta (LOGIC_PORT). Separate comfort limits from hard allocation safety; do not cap
   strong devices; no permanently stripped Lite product.
3. Upstream observation: official `7d3cea0` (#3740, audio delay UI) is not accepted; sync at a
   later gate boundary unless a G2 owner changes upstream.

## Evidence / limits
Local Android execution is unavailable (no Android SDK); use GitHub CI evidence. Actions artifacts
cannot be downloaded here (blob storage egress blocked); the baseline-audit workflow prints the
inventory to the job log. No hardware/manual tests run or claimed. MANUAL-PENDING from G1: TV focus
of the add-on badge; on-device HUD values (refresh, passthrough, DV); D-pad focus/apply/revert on
the device assessment card.

## Ownership / unfinished work
Sequential writer, no lease. Admin actions (default branch, protection) remain in
GITHUB_ADMIN_CHECKLIST; connector access to those settings is not available.
