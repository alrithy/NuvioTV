# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G3 — Playback Strategy Framework
- Active branch: `feat/playback-strategies`
- Status: IN_PROGRESS
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
G3 IN_PROGRESS. G3a (strategy model, Auto resolver, session-only settings adapter, selection
card, HUD row; features 3, 27–31; D043) is on `feat/playback-strategies`. Audit:
`docs/audits/G3_PLAYBACK_STRATEGY_AUDIT.md`. No external import (official + project glue). Local
evidence: JVM harness for `fork/` (76 tests, 0 failures; PlaybackStrategyTest 14);
PlaybackStrategySettingsTest and the official call sites run only in CI.

## Exact next action
1. Drive the G3a PR to green (full-suite guard, fullDebug) and squash-merge it.
2. G3 closeout (like #13/#17): G3 DONE, activate G4 REMUX / Network Performance
   (`feat/remux-network`); 280 stays deferred (MPV memory measurement needed).
3. Upstream: official `7d3cea0` (#3740) is not needed by G3 (no edit to `AudioSelectionOverlay` or
   `PlayerRuntimeControllerPlaybackEvents`); re-check official before G4, which edits the
   player/media-source/network owners.

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
