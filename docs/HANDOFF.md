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
G4 IN_PROGRESS (user chose option 2, 2026-09-29: build every slice, keep hardware A/B
MANUAL-PENDING, G4 not DONE until the same-file A/B runs on the user's TCL C6K). Audit merged (PR #20,
`5a6a308`; 4, 10, 12, 13 verified_official). G4a merged (PR #21, `12ca7ba`): dead-source failover +
startup watchdog (19, 20, D044). G4b merged (PR #22, `8cb77f9`): press-time warm-up for REMUX (18);
8, 9, 11 verified_official. G4c merged (PR #24, `e052568`): Matroska resync to the next Cluster on
ParserException only (21, AR-008); 22, 23 verified_official. On `feat/remux-network`: architect
review follow-ups AR-001..AR-007 (safe failover logs, monotonic watchdog clock, D046 keeps
REMUX_PERFORMANCE AUTO, feature 3 in_progress, feature 77 deferred to G5, press-time MIME guard).
Responses are posted on PR #23 (queue doc, owner's PR, not merged yet).

## Exact next action
1. Drive the review follow-ups PR to green and squash-merge; merge integration back.
2. When PR #23 lands, copy the AR-001..AR-008 responses into `docs/ARCHITECT_REVIEW_QUEUE.md`.
3. G4d: Seek optimized adds ysosrs MP4 session mode (24) for progressive MP4 and the Reshaped disk
   read-ahead ring (25) for other progressive files, never on the parallel path (D006, D045);
   5, 6, 17, 26 deferred, 7 verified_official; plus `docs/HARDWARE_VALIDATION_TCL_C6K.md`.
4. Do not mark G4 DONE before the same-file A/B in that checklist runs on the TCL C6K.

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
