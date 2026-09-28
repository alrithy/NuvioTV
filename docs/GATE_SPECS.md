# Gate Specifications

Primary traceability: `integration/feature_traceability.csv`.

## G0 — Fork Foundation
IDs: 1, 2, 311, 313–315, 317–319.
Goal: governance, feature registry, attribution, agent workflow and CI with no user-visible playback/UI change.
Exit: validator + fullDebug unit tests + assemble pass.

## G1 — Unified Diagnostics & Add-on Health
IDs: 14–16, 61–81, 281–291.
Sources: official + ysosrs delta.
Owner: official PlayerDebugStatsOverlay / one normalized model.
Exit: metrics degrade gracefully; no hot-path polling regression.

## G2 — Adaptive Resource Manager
IDs: 262–280.
Source: Lite logic.
Exit: deterministic tiers; weak devices capped; strong devices not over-restricted.

## G3 — Playback Strategy Framework
IDs: 3, 27–31.
Goal: Official / REMUX / Seek Optimized / Low Memory / Auto.
Exit: official path unchanged and fallback-safe.

## G4 — REMUX / Network Performance
IDs: 4–13, 17–26.
Sources: official + ysosrs/Reshaped deltas.
Mandatory same-file A/B.
Exit: no unexplained regression in startup/rebuffer/waste/RAM.

## G5 — Audio / DV / HDR / AFR
IDs: 32–44, 46–60.
MAT ID 45 is G13.
Exit: capability fallback safe on unsupported hardware.

## G6 — Subtitle Intelligence
IDs: 82–106.
Pipeline: embedded -> hash/release -> cue-rhythm -> audio/ASR -> manual.
Exit: Arabic/language-independent cases, confidence fail-closed, credential-safe.

## G7 — Seek Intelligence
IDs: 107–116.
Exit: preview aligns with seek; weak calibration rejected; memory bounded.

## G8 — Stream Intelligence
IDs: 147–168.
Exit: progressive fallback works; one ranker powers list/autoplay.

## G9 — Skip / Recommendations / Discovery
IDs: 117–146, 169–187, 206–207.
Exit: provider isolation/timeouts, spoiler-safe flows, no duplicate engines.

## G10 — Live TV
IDs: 208–236.
Source: Reshaped.
Exit: M3U/Xtream/Stalker, remote focus, dead-source isolation, resource-aware previews.

## G11 — Watch Party
IDs: 237–249.
Source: Antonino.
Security exit: explicit consent, redaction, no persistence, cleanup, encrypted transport.

## G12 — UI Styles & Screensaver
IDs: 188–205, 292.
Exit: official layouts retained; D-pad and weak-device fallback verified.

## G13 — Experimental
IDs: 45, 250–261.
MAT + AI, OFF by default.
Exit: disabling removes runtime behavior; keys/provider identity protected.

## G14 — Hardening, Distribution & Upstream Operations
IDs: 293–310, 312, 316, 320.
Exit: release/upgrade/security/license/upstream checks and all 320 IDs accounted for.
