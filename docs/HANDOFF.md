# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G4 — REMUX / Network Performance
- Active branch: `feat/remux-network`
- Status: IN_PROGRESS
- Task: `tasks/G4_REMUX_NETWORK.md`
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
G4 IN_PROGRESS: mandatory audit done (`docs/audits/G4_REMUX_NETWORK_AUDIT.md`); 4, 10, 12, 13 are
verified_official. No G4 code yet. Slice plan: G4a failover + startup watchdog (19, 20, ysosrs),
G4b ParallelRangeDataSource refinements (8, 9, 11, ysosrs, opt-in via G3 REMUX), G4c container
recovery (21–24, fixtures), G4d Reshaped disk seek buffer (25, Seek optimized only).

## Exact next action
1. **Waiting on the user (hardware):** GATE_SPECS G4 exit needs same-file/source A/B on a real
   device (startup, throughput, rebuffer, waste, RAM, seek). Ask whether they can run it and on which
   device, or whether to build the slices with the A/B left MANUAL-PENDING.
2. Then G4a on `feat/remux-network`: port ysosrs `attemptDeadSourceFailover`,
   `advanceToNextLiveSource`, `attemptStartupExhaustedSourceFailover`, startup watchdog; keep the
   decision logic pure and unit-tested. Check first whether it edits
   `PlayerRuntimeControllerPlaybackEvents` (then sync official `7d3cea0` #3740 in a separate PR).

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
