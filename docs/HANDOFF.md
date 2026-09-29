# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G1 — Unified Diagnostics & Add-on Health
- Active branch: `feat/unified-diagnostics`
- Status: IN_PROGRESS
- Task: `tasks/G1_UNIFIED_DIAGNOSTICS.md`
- Accepted official baseline: `71632b9271e8bce6783e415d64f34cfa4e8b894c`
- Governance: READY
- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.
<!-- canonical-state:end -->

## Current work boundary
G0 DONE (PR #7). Upstream sync to official `71632b9` merged (PR #9, merge `a909827`, D040).
G1 is IN_PROGRESS on `feat/unified-diagnostics`, delivered as sequential PRs from this branch:
- **G1a add-on health (this PR):** 281–287, 291 implemented; 288, 290 verified_official;
  289 deferred to G8 (D041). Audit: `docs/audits/G1_DIAGNOSTICS_AUDIT.md`.
- **G1b (next):** diagnostics model in official `PlayerDebugStatsOverlay` — features 61–81.
- **G1c:** device assessment + apply/revert — features 14–16.
G1 is DONE only after G1c merges and every G1 row has a terminal status.

## G1a delivered
- `fork/diagnostics/AddonHealth.kt`: states + classifier (ALGORITHM_PORT from ysosrs 45e0984).
- `fork/diagnostics/AddonHealthTracker.kt`: in-memory, keyed by canonical add-on base URL;
  records nothing when UNIFIED_DIAGNOSTICS is OFF. No URLs/headers/messages stored or logged.
- `fork/foundation/ForkFoundationModule.kt`: Hilt provider for FeatureRegistry.
- FeatureRegistry: `DECIDED_DEFAULTS` = UNIFIED_DIAGNOSTICS→AUTO (D041); all others OFF.
- Hooks: `AddonRepositoryImpl.fetchAddon` (manifest), `StreamRepositoryImpl` per-add-on stream
  fetch; badge in `AddonManagerScreen` cards via `AddonManagerViewModel`/`UiState.healthByUrl`.
- Strings: `addon_health_*` in values + values-ar.
- Tests: AddonHealthTest (10), StreamRepositoryAddonHealthTest (2), FeatureRegistryTest (9),
  SourceAttributionTest (4). Fork-package tests pass in a local Kotlin/JVM harness; Android
  compile/full suite/APK are verified only by GitHub CI.

## Exact next action
1. Drive the G1a PR to green (exact-head CI) and merge it (squash).
2. Continue on `feat/unified-diagnostics` (merge integration first) with G1b: read the ysosrs
   `PlaybackStatsOverlay`/`PlayerViewModel.samplePlaybackStats` samplers at 45e0984, port only
   metrics missing from official `PlayerDebugStatsOverlay`, keep it the single HUD, sample off
   the Compose hot path, show unavailable values as unavailable. 81 stays unavailable until G3.
3. Then G1c device assessment (`core/assessment/*`) with apply/revert and tests.

## Evidence / limits
Local Android execution is unavailable (no Android SDK); use GitHub CI evidence. Actions
artifacts cannot be downloaded from this agent environment (blob storage egress blocked);
the baseline-audit workflow prints the safe inventory to the job log instead.
No hardware/manual tests run or claimed (TV focus of the new badge: MANUAL-PENDING).

## Ownership / unfinished work
Sequential writer, no lease. Admin actions (default branch, protection) remain in
GITHUB_ADMIN_CHECKLIST; connector access to those settings is not available.
