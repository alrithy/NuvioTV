# Gate Checklists

## Gate 0 — Fork Foundation
- [ ] Baseline ancestry verified.
- [ ] FeatureId and FeatureMode created.
- [ ] FeatureRegistry defaults preserve official behavior.
- [ ] SourceAttribution utility exists.
- [ ] Import ledger exists.
- [ ] Unit tests for defaults pass.
- [ ] No user-visible behavior changed.
- [ ] state/handoff updated.

## Gate 1 — Unified Diagnostics
- [ ] Official PlayerDebugStatsOverlay remains UI owner.
- [ ] ysosrs metrics audited against current official.
- [ ] Only missing metrics imported.
- [ ] One normalized diagnostics model.
- [ ] Metrics have unavailable/unsupported states.
- [ ] No sensitive URL/header leakage.
- [ ] Playback overhead measured/acceptable.

## Gate 2 — Adaptive Resource Manager
- [ ] Physical RAM/device-tier detection.
- [ ] Comfort vs hard-safety limits separated.
- [ ] Buffer/connections/chunks/cache policies centralized.
- [ ] Large devices retain high-performance paths.
- [ ] Constrained-device tests.
- [ ] No permanent Lite feature deletion.

## Gate 3 — Playback Strategy Framework
- [ ] Official, REMUX, Seek Optimized, Low Memory and Auto strategies exist.
- [ ] Explicit selection/fallback exists.
- [ ] Strategy state is visible to diagnostics.

## Gate 4 — REMUX Performance Delta
- [ ] ysosrs delta audited against current official.
- [ ] No duplicate official parallel-range behavior.
- [ ] 429/503/stall behavior evaluated.
- [ ] No re-download regression.
- [ ] High-bitrate same-file A/B completed.
- [ ] Source failover/container recovery isolated and tested.
- [ ] Add-on health ownership defined if imported here.

## Gate 5 — Audio / DV / AFR
- [ ] Per-format passthrough controls.
- [ ] Lossless selection/fallback.
- [ ] Output diagnostics.
- [ ] Downmix path checked.
- [ ] DV behavior audited against official libdovi.
- [ ] Frame-rate/eARC behavior tested.
- [ ] MAT excluded from stable gate.

## Gate 6 — Subtitle Intelligence
- [ ] One sync engine only.
- [ ] Embedded reference.
- [ ] Release/hash reference.
- [ ] Cue-rhythm alignment.
- [ ] Drift/scale correction.
- [ ] Confidence fail-closed.
- [ ] Arabic and generic-language tests.
- [ ] Audio/ASR optional fallback.
- [ ] Custom fonts / secondary language.
- [ ] Header/credential forwarding safety.

## Gate 7 — Seek Intelligence
- [ ] Local keyframes first.
- [ ] Seekr fallback.
- [ ] Calibration and weak-match rejection.
- [ ] Disk seek buffer is a strategy, not blindly stacked.
- [ ] RAM/disk bounds.
- [ ] Preview/actual seek alignment tests.

## Gate 8 — Stream Intelligence
- [ ] Progressive capability detection and ordinary fallback.
- [ ] Dedup.
- [ ] Cached/uncached handling.
- [ ] Quality/release/source/connection ranking.
- [ ] Addon order preserved when no strong override.
- [ ] Autoplay uses the same ranker.

## Gate 9 — Discovery / Skip / Recommendations
- [ ] One skip aggregator.
- [ ] Providers isolated with timeouts.
- [ ] Evidence/confidence merge.
- [ ] Skip vs mute distinct.
- [ ] Calendar.
- [ ] Random / Mystery.
- [ ] Recommendation providers.
- [ ] App Dimmer.
- [ ] Spoiler-safe behavior.

## Gate 10 — Live TV
- [ ] M3U, Xtream, Stalker and multiple sources.
- [ ] QR setup.
- [ ] EPG / Now / Next.
- [ ] Categories/favorites/search/hide/reorder.
- [ ] Preview resource bounds.
- [ ] Zapping and remote focus.
- [ ] Live retries / MPEG-TS startup / AFR.

## Gate 11 — Watch Party
- [ ] PlayerBridge integration.
- [ ] Room create/join/leave.
- [ ] Play/pause/seek.
- [ ] Soft/hard drift correction.
- [ ] Explicit URL/header-sharing consent.
- [ ] No persistence/logging of secrets.
- [ ] Cleanup.
- [ ] Third-party notices audited.

## Gate 12 — UI Styles
- [ ] Official layouts preserved.
- [ ] Glass selectable.
- [ ] Pill/top-nav selectable.
- [ ] Low-capability fallback.
- [ ] D-pad/focus matrix.
- [ ] No navigation duplication.

## Gate 13 — Experimental / Release
- [ ] AI provider architecture isolated.
- [ ] BYOK credentials in Keystore.
- [ ] Provider hash/signer verification.
- [ ] MAT OFF by default.
- [ ] Stable/beta channel plan.
- [ ] ABI outputs.
- [ ] Updater.
- [ ] CI/release validation.
