# Integration Roadmap

Implementation order is controlled here. Detailed entry/exit criteria are in `docs/GATE_SPECS.md`. Every one of the 320 feature IDs is mapped in `integration/feature_traceability.csv`.

## G0 — Fork Foundation
Governance, feature registry, source attribution, project CI and agent-safe workflow. No user-visible playback/UI behavior change.

## G1 — Unified Diagnostics & Add-on Health
One diagnostics model/UI owner, device assessment, advanced playback metrics and add-on health.

## G2 — Adaptive Resource Manager
RAM/device tiers, bounded caches/concurrency and resource-aware behavior.

## G3 — Playback Strategy Framework
Official / REMUX / Seek Optimized / Low Memory / Auto.

## G4 — REMUX / Network Performance
Port only current missing deltas from ysosrs/Reshaped. Same-file A/B is mandatory for final hardware certification but does not block subsequent development gates.

## G5 — Audio / DV / HDR / AFR
Lossless passthrough, output diagnostics, DV/HDR/AFR deltas. MAT stays experimental.

## G6 — Subtitle Intelligence
Embedded -> hash/release -> cue rhythm -> audio/ASR -> manual fallback. Arabic and language-independent behavior.

## G7 — Seek Intelligence
Local keyframe previews, calibrated Seekr fallback and bounded seek-buffer behavior.

## G8 — Stream Intelligence
Progressive AIOStreams, dedup, cache/quality analysis, connection fit, ranking and autoplay.

## G9 — Skip / Recommendations / Discovery
Multi-provider skip, Calendar, Random/Mystery, post-play providers and App Dimmer.

## G10 — Live TV
M3U/Xtream/Stalker, multi-source, QR, EPG, categories, favorites, previews, zapping and Now/Next.

## G11 — Watch Party
PlayerBridge-based synchronized playback with URL/header security hardening.

## G12 — UI Styles & Screensaver
Glass/Pill/top navigation and related visual options while preserving official layouts.

## G13 — Experimental
AI Media Providers and MAT/IEC61937, OFF by default.

## G14 — Hardening, Distribution & Upstream Operations
Consolidated hardware certification for all MANUAL-PENDING checks, self-host validation, security closure, updater/release channels, APK integrity, upstream-sync automation and final 320-feature accounting.

Development gates G1–G13 may advance under `docs/DEFINITION_OF_DONE.md` with device-only checks recorded as MANUAL-PENDING. G14 is the stop point for the single consolidated hardware-validation campaign before Stable release.