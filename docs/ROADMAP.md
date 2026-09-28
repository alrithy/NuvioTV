# Integration Roadmap

## Gate 0 — Foundation
Feature registry, source attribution, import ledger, tests. No user-visible behavior changes.

## Gate 1 — Unified diagnostics
Use official PlayerDebugStatsOverlay as owner. Add only missing metrics from ysosrs.

## Gate 2 — Adaptive Resource Manager
Consolidate Lite RAM/device-tier logic into one policy.

## Gate 3 — Playback Strategy framework
Official / REMUX / Seek Optimized / Low Memory / Auto. Official remains default/fallback.

## Gate 4 — REMUX performance delta
Diff ysosrs against current official and port only missing behavior. Mandatory same-file A/B.

## Gate 5 — Audio / DV / AFR
Port only missing passthrough, lossless, DV and AFR deltas. MAT remains experimental.

## Gate 6 — Subtitle Intelligence
Unify Reshaped + VibeSubtitle into one engine: embedded -> hash/release -> cue rhythm -> audio/ASR -> manual fallback. Add custom fonts and secondary language.

## Gate 7 — Seek Intelligence
Local keyframe previews first, Seekr fallback/calibration, disk seek buffer as a distinct strategy.

## Gate 8 — Stream Intelligence
Progressive AIOStreams, dedup, cache state, quality analysis, connection-fit, ranking/autoplay.

## Gate 9 — Discovery / Skip / Recommendations
Multi-provider skip aggregator, Calendar, Random/Mystery, post-play provider selector, App Dimmer.

## Gate 10 — Live TV
Port Reshaped M3U/Xtream/Stalker, multi-source, QR setup, EPG, categories, search, favorites, previews, zapping, Now/Next.

## Gate 11 — Watch Party
Port through PlayerBridge. Explicit permission before sharing stream URLs/headers. Redact secrets and clean sessions.

## Gate 12 — UI styles
Glass, Pill and top navigation as selectable styles. Never remove official layouts.

## Gate 13 — Experimental
AI Media Providers and MAT/IEC61937. OFF by default.

A gate is complete only when attribution is recorded, tests pass, fallback/flag exists where practical, regression checks pass, and source SHA/commits are recorded.
