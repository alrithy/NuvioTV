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
- **G1a add-on health (PR #10, merged `d8511d8`):** 281–287, 291 implemented; 288, 290
  verified_official; 289 deferred to G8 (D041). Audit: `docs/audits/G1_DIAGNOSTICS_AUDIT.md`.
- **G1b diagnostics HUD (this PR):** features 61–81 — rows appended to official
  `PlayerDebugStatsOverlay` from `fork/diagnostics/PlaybackHud.kt`; 63, 70, 72, 75, 78, 80 verified_official.
- **G1c:** device assessment + apply/revert — features 14–16.
G1 is DONE only after G1c merges and every G1 row has a terminal status.

## G1a delivered (merged)
In-memory `AddonHealthTracker`, classifier, Addon Manager badge, FeatureRegistry D041 default.

## G1b delivered
- `fork/diagnostics/PlaybackHud.kt`: pure rows for video, hdr, display, audio, output, need,
  conn, rebuffer, underrun, load err, a-clock, soc, strategy + `ClockDriftMeter`.
- Official `PlayerPlaybackAnalyticsDiagnostics`: reduced `HudSample` + AudioTrack output encoding
  (`onAudioTrackInitialized` listener in PlayerRuntimeControllerInitialization).
- Official `ParallelRangeDataSource`: `hudConnections`/`hudChunkBytes` set on open, cleared on reset.
- `PlayerViewModel`: `forkDiagnosticsEnabled`, `getPlaybackHudSample()`, `getRebufferCount()`.
- `PlayerDebugStatsOverlay`: appends G1b rows (read on the main thread in the existing 1 Hz loop).
- Tests: PlaybackHudTest (11) passes in the local Kotlin/JVM harness (media3 constants stubbed
  there only); Android compile/full suite/APK verified only by GitHub CI.

## Exact next action
1. Drive the G1b PR to green (exact-head CI) and merge it (squash).
2. Merge integration into `feat/unified-diagnostics`, then G1c: device assessment + apply/revert
   (features 14–16) from ysosrs `core/assessment/*` and `DeviceAssessmentScreen` at 45e0984; audit
   official settings owners first; revert must restore exact previous values; tests required.
3. After G1c merges: every G1 row terminal → closeout PR sets G1 DONE and activates G2.

## Evidence / limits
Local Android execution is unavailable (no Android SDK); use GitHub CI evidence. Actions
artifacts cannot be downloaded from this agent environment (blob storage egress blocked);
the baseline-audit workflow prints the safe inventory to the job log instead.
No hardware/manual tests run or claimed. MANUAL-PENDING: TV focus of the add-on badge; on-device HUD values (refresh, passthrough, DV) for G1b.

## Ownership / unfinished work
Sequential writer, no lease. Admin actions (default branch, protection) remain in
GITHUB_ADMIN_CHECKLIST; connector access to those settings is not available.
