# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G4 — REMUX / Network Performance
- Active branch: `feat/remux-network`
- Status: BLOCKED
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
G4 BLOCKED on hardware only (user chose option 2, 2026-09-29: build every slice, keep hardware A/B
MANUAL-PENDING, G4 not DONE until the same-file A/B runs on the user's TCL C6K). All G4 code is merged:
audit (PR #20, `5a6a308`), G4a failover + watchdog (PR #21, `12ca7ba`; 19, 20, D044), G4b press-time
warm-up (PR #22, `8cb77f9`; 18), G4c Matroska resync (PR #24, `e052568`; 21, AR-008), review
follow-ups (PR #25, `e56bb37`; AR-001..AR-007, D046), G4d Seek optimized MP4 session + read-ahead
ring (PR #26, `feb0f8f`; 24, 25, D045). verified_official: 4, 7, 8, 9, 10, 11, 12, 13, 22, 23.
Deferred with reasons: 5, 6, 17, 26. Every G4 row has an honest terminal status; the only open
item is the exit A/B.

## Exact next action
1. The user runs `docs/HARDWARE_VALIDATION_TCL_C6K.md` on the TCL C6K (sections 1-5) and shares the
   logs and the A/B table. Record each result as PASS / FAIL / MANUAL-PENDING in
   `docs/MANUAL_TEST_LOG.md`; close G4 (DONE) only if section 5 passes, otherwise fix or record a
   decision per the checklist's exit criterion.
2. When PR #23 (owner's review queue) lands, copy the AR-001..AR-008 responses into
   `docs/ARCHITECT_REVIEW_QUEUE.md`.
3. G5 depends on G4 in `integration/task_queue.csv`; do not start G5 before G4 is DONE unless the
   user explicitly decides to.

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
