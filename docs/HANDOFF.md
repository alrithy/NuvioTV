# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G3 — Playback Strategy Framework
- Active branch: `feat/playback-strategies`
- Status: READY
- Task: `tasks/G3_PLAYBACK_STRATEGIES.md`
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
G3 Playback Strategy Framework is READY; no G3 code yet.

## Exact next action
1. Create `feat/playback-strategies` from integration HEAD; run
   `python3 scripts/superfork/preflight.py --fetch`; read `tasks/G3_PLAYBACK_STRATEGIES.md` and
   GATE_SPECS G3.
2. Upstream first: official `7d3cea0` (#3740) edits `AudioSelectionOverlay` and
   `PlayerRuntimeControllerPlaybackEvents`. If G3 will edit either, adopt it through a separate
   reviewed sync PR before feature work (never mixed with a port); otherwise leave it observed.
3. Audit official first (PlayerMediaSourceFactory, ParallelRangeDataSource, performance mode,
   NuvioExoPlayerPerformanceHelper, MPV engine) and classify 3, 27–31. Strategies: Official /
   REMUX-Throughput / Seek Optimized / Low Memory / Auto; Official stays the fallback; do not
   stack the Reshaped seek buffer with ysosrs parallel REMUX; Low Memory reads
   `AdaptiveResourcePolicy` and owns the MPV demuxer budget (280); diagnostics show the
   selected/effective strategy through the G1 HUD `strategy` row.

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
