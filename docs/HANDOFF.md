# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G5 — Audio / DV / HDR / AFR
- Active branch: `feat/audio-video`
- Status: IN_PROGRESS
- Task: `tasks/G5_AUDIO_VIDEO.md`
- Accepted official baseline: `71632b9271e8bce6783e415d64f34cfa4e8b894c`
- Governance: READY
- Owner of these fields: `integration/state.yaml`; regenerate with `state_view.py`.
<!-- canonical-state:end -->

## Current work boundary
G0 DONE (PR #7). Official `71632b9` accepted (PR #9, D040).
G1 DONE (PRs #10–#12, `27c921c`; 289 deferred to G8, D041).
G2 DONE: G2a tiers + fan-out/image/post-play (PR #14, `b2a13d1`), G2b playback allocation safety
(PR #15, `9002d54`; debt 19 → 16), G2c bounded caches, stream-list and bidi work (PR #16,
`52ffbd3`). D042 (ADAPTIVE_RESOURCE_MANAGER AUTO). Deferred: 276 (no offline queue exists),
279 → G7 (Seekr), 280 → G3 (MPV cache in the Low Memory strategy). Audit:
`docs/audits/G2_RESOURCE_AUDIT.md`; provenance: IMPORT_LEDGER G2a/G2b/G2c. One resource owner:
`fork/resource/AdaptiveResources` (`AdaptiveResourcePolicy`).
G3 DONE: G3a strategies (PR #18, `0a64cde`): Official / REMUX-Throughput / Seek optimized /
Low memory / Auto as session-only overrides of official buffer/network settings, per-profile
selection (default Official, D043), HUD `strategy` row. Audit:
`docs/audits/G3_PLAYBACK_STRATEGY_AUDIT.md`. One strategy owner: `fork/playback`
(`PlaybackStrategies`, `PlaybackStrategySession`). 280 stays deferred (MPV memory measurement).
G4 implementation CODE-COMPLETE: all slices merged (PRs #21, #22, #24, #25, #26) with 0 new CI failures and the debug APK built. Its TCL C6K functional checks and same-file A/B remain MANUAL-PENDING and are intentionally batched with all other device-only checks for G14 final hardware certification. G4 therefore no longer blocks development.

G5 IN_PROGRESS on `feat/audio-video`: audit `docs/audits/G5_AUDIO_VIDEO_AUDIT.md` (official has
TrueHD/Atmos/DD+/DTS-HD passthrough via live platform capabilities, downmix, FFmpeg fallback, 0-10 dB boost, libdovi, DV7/DV5 to 8.1, AFR
modes; gaps: per-format passthrough controls,
lossless default, soft clip, HDR10 on the DV strip path, track-format AFR/settle). 46 deferred.
D047 records the batched-validation policy of `9c627ca`.
Audit merged (PR #31, `7ccb762`). G5a soft clip for boosted volume (48, Reshaped `softClipBoosted`) in review; D048 decides AUDIO_DV_AFR AUTO with official output kept by default.

## Exact next action
1. Merge G5a, then slices G5b (lossless default, 37), G5c
   (per-format passthrough controls + capability report, 38-42), G5d (DV/HDR, 52-55, 60), G5e (AFR,
   56, 58, 59). No slice edits AudioSelectionOverlay / PlaybackEvents without a prior upstream sync.
2. Continue G5→G13 without stopping for device-only MANUAL-PENDING checks. Record each one honestly in the gate docs/manual-test log and carry it into `validation_pending_gates`.
3. In G14, stop once for the consolidated hardware-certification campaign and execute all accumulated device/manual checks, including `docs/HARDWARE_VALIDATION_TCL_C6K.md`.
4. PR #28 is the governance review-queue PR; it is independent of G5 runtime work.
5. Append every G5 device check to `docs/HARDWARE_VALIDATION_TCL_C6K.md` (MANUAL-PENDING, G14).

## Evidence / limits
Local Android execution is unavailable (no Android SDK); use GitHub CI evidence. Actions artifacts
cannot be downloaded here (blob storage egress blocked); the baseline-audit workflow prints the
inventory to the job log. No hardware/manual tests run or claimed. MANUAL-PENDING from G1: TV focus
of the add-on badge; on-device HUD values (refresh, passthrough, DV); D-pad focus/apply/revert on
the device assessment card. MANUAL-PENDING from G2: memory/scrolling (G2a) and startup/seek with the lower budget (G2b) on
1–2 GB boxes; source-panel focus smoothness and RTL rendering (G2c). MANUAL-PENDING from G3: D-pad focus on the
strategy card; each strategy's playback on real REMUX/HLS files (HUD `strategy` row).

## Ownership / unfinished work
Sequential writer, no lease. Admin actions (default branch, protection) remain in
GITHUB_ADMIN_CHECKLIST; connector access to those settings is not available.