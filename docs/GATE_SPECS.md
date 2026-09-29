# Gate Specifications

Primary feature traceability: integration/feature_traceability.csv.
Global completion rules: docs/DEFINITION_OF_DONE.md.

A gate may not be declared complete merely because code compiles. All applicable deliverables and exit checks below must be satisfied, and feature statuses must be updated in traceability.

Device and hardware exit checks (A/B runs, real-device playback, TV focus/feel, DV/HDR/passthrough/AFR output) run once in the final validation stage (G14 exit) per D047. A gate closes on its code, test, CI and traceability checks; each device check it leaves stays MANUAL-PENDING in `docs/HARDWARE_VALIDATION_TCL_C6K.md`.

## G0 — Fork Foundation
IDs: 1, 2, 311, 313–315, 317–319
Primary source: project/official baseline
User-visible behavior: must remain unchanged.

Required deliverables:
- com.nuvio.tv.fork.foundation package.
- FeatureId and FeatureMode with OFF, ON, AUTO.
- FeatureRegistry with defaults preserving official behavior.
- SourceAttribution model/utility.
- Fork-only settings store only if actually required; do not prematurely migrate official settings.
- Governance validator/CI operational on Superfork PRs.
- Source attribution/import-ledger workflow.
- Unit tests for feature default semantics.

Exit:
- governance validator PASS;
- complete fullDebug suite executed; no new failures under BASELINE_TEST_DEBT;
- fullDebug assemble PASS;
- no user-visible playback/UI change;
- G0 feature rows updated honestly;
- state, project status and handoff updated.

## G1 — Unified Diagnostics & Add-on Health
IDs: 14–16, 61–81, 281–291
Sources: official + ysosrs delta
Owner: official PlayerDebugStatsOverlay + one normalized diagnostics model.

Required:
- audit current official metrics first;
- import only missing ysosrs metrics/assessment/health behavior;
- device assessment + apply/revert;
- add-on health states and failure isolation;
- unavailable metrics degrade gracefully;
- no credential/signed-URL leakage;
- avoid hot-path polling/judder regressions.

Exit: focused diagnostics tests + playback overhead sanity + fullDebug tests/build.

## G2 — Adaptive Resource Manager
IDs: 262–280
Source: hackerslash Lite logic.

Required:
- physical RAM/device tier detection;
- separate comfort limits from hard allocation-safety limits;
- central policies for buffer, parallelism, chunk/cache sizes and expensive previews;
- strong devices are not unnecessarily restricted;
- constrained-device tests.

Exit: deterministic tiers, bounded resources, fullDebug green.

## G3 — Playback Strategy Framework
IDs: 3, 27–31
Sources: official + project glue.

Required strategies:
- Official
- REMUX / Throughput
- Seek Optimized
- Low Memory
- Auto

Required:
- official path remains unchanged/fallback-safe;
- explicit selection model;
- Auto uses device/network/media signals without hardcoding one fork globally;
- diagnostics expose selected/effective strategy.

Exit: strategy selection tests + official fallback tests + fullDebug green.

## G4 — REMUX / Network Performance
IDs: 4–13, 17–26
Sources: official + ysosrs + Reshaped deltas.

Required:
- file-level diff against current official ParallelRangeDataSource, StreamSpeedTester, media source and recovery paths;
- no duplicate upstream implementation;
- evaluate adaptive connections/chunks, deep buffering, rate-limit handling, stall recovery, pre-resolve/warmup, failover, container recovery, disk seek-buffer interaction;
- disk seek-buffer and parallel REMUX paths remain coordinated strategies, not blindly stacked.

Exit:
- mandatory same-file/source A/B (final validation stage, D047);
- no unexplained regression in startup, rebuffer, waste, RAM or seek (final validation stage, D047);
- malformed/non-faststart fixtures where applicable;
- fullDebug green.

## G5 — Audio / DV / HDR / AFR
IDs: 32–44, 46–60
Sources: official + ysosrs/Reshaped deltas
MAT ID 45 belongs to G13.

Required:
- per-format passthrough controls;
- lossless selection/fallback;
- output/device capability diagnostics;
- downmix fallback;
- diff DV/libdovi behavior against current official;
- frame-rate/eARC settle behavior;
- unsupported hardware remains safe.

Exit: codec/capability tests + honest device MANUAL-PENDING/PASS state + fullDebug green.

## G6 — Subtitle Intelligence
IDs: 82–106
Sources: Reshaped + VibeSubtitle.

Single pipeline:
embedded reference -> hash/release reference -> cue-rhythm -> audio/ASR -> manual/original timing.

Required:
- one engine only;
- offset + scale + piecewise drift support where justified;
- confidence score and fail-closed behavior;
- Arabic and language-independent cases;
- secondary language;
- custom TTF/OTF flow with validation/fallback;
- safe header/credential forwarding;
- no unnecessary video reload for sidecar switching.

Exit: aligned/fixed-offset/drift/low-confidence/Arabic tests + fullDebug green.

## G7 — Seek Intelligence
IDs: 107–116
Sources: Reshaped + Cxsmo.

Required:
- local keyframe preview first;
- calibrated Seekr fallback;
- manual adjustment;
- weak calibration rejection;
- bounded RAM/disk;
- preview timestamp aligns with actual seek target.

Exit: seek alignment/memory tests + long-file regression + fullDebug green.

## G8 — Stream Intelligence
IDs: 147–168
Sources: official + Reshaped + Cxsmo.

Required:
- progressive capability detection;
- normal AIOStreams fallback;
- cumulative result dedup;
- one ranker for list + autoplay;
- cached RD before uncached while keeping uncached visible;
- quality/release/source reliability/codec/HDR/audio/bitrate/connection-fit signals;
- preserve addon order absent a strong reason to override.

Exit: deterministic ranking tests + fallback tests + fullDebug green.

## G9 — Skip / Recommendations / Discovery
IDs: 117–146, 169–187, 206–207
Sources: official + Cxsmo.

Required:
- one skip aggregator;
- provider isolation/timeouts;
- evidence/confidence merge;
- skip vs mute actions remain distinct;
- ID normalization;
- Calendar with spoiler-safe behavior;
- Random/Mystery episode flows;
- post-play provider selection/pagination/lazy metadata;
- App Dimmer.

Exit: provider failure tests + spoiler/focus regression + fullDebug green.

## G10 — Live TV
IDs: 208–236
Primary source: Reshaped.

Required:
- M3U, Xtream, Stalker;
- multiple sources + QR setup;
- EPG/Now/Next/progress;
- categories/favorites/search/hide/reorder;
- channel preview with resource bounds;
- zapping/remote focus;
- dead-source isolation/retry;
- MPEG-TS startup and live-aware AFR.

Exit: source/parser tests + TV focus/zapping tests + manual provider coverage status + fullDebug green.

## G11 — Watch Party
IDs: 237–249
Primary source: Antonino Watch Party.

Required:
- PlayerBridge integration;
- room create/join/leave;
- play/pause/seek sync;
- soft/hard drift correction;
- explicit user consent before sharing resolved URL/required headers;
- never persist/log sensitive URL/header values;
- cleanup room/session state;
- preserve encrypted transport behavior;
- audit third-party notices.

Exit: sync/drift/security/cleanup tests + fullDebug green.

## G12 — UI Styles & Screensaver
IDs: 188–205, 292
Sources: official + NuvioGlass + Reshaped + Cxsmo/ysosrs where relevant.

Required:
- official layouts remain selectable;
- Glass/Pill/top-nav are options, not replacements;
- low-capability visual fallback;
- screensaver optional;
- D-pad/focus/back navigation matrix;
- no duplicate navigation owner.

Exit: Compose/UI tests where practical + manual TV focus status + fullDebug green.

## G13 — Experimental
IDs: 45, 250–261
Sources: ysosrs + Fornace.

Required:
- MAT and AI remain OFF by default;
- AI providers isolated from core app;
- BYOK/profile-scoped Keystore storage;
- provider SHA-256 and signer verification;
- credentials absent from logs/session URLs;
- disabling experimental features removes their runtime behavior.

Exit: security/config tests + fullDebug green; experimental status explicitly recorded.

## G14 — Hardening, Distribution & Upstream Operations
IDs: 293–310, 312, 316, 320
Sources: official + project.

Required:
- validate official self-host/server-discovery ownership rather than re-port old Enhanced code;
- server trust/security closure;
- release/updater/stable/beta channel hardening;
- ABI artifacts/checksums;
- fresh-install + upgrade path;
- upstream-sync automation/process;
- license/import-ledger audit;
- all 320 feature rows accounted for as implemented, verified_official, blocked, deferred or explicitly experimental;
- final validation stage (D047): every row of `docs/HARDWARE_VALIDATION_TCL_C6K.md` run on the TCL C6K and recorded PASS/FAIL in `docs/MANUAL_TEST_LOG.md`; FAILs fixed or decided before release.

Exit:
- release policy satisfied, including Stable/Beta updater, ABI APKs, checksums, provenance/license, automated upstream checks and final regression certification;
- security review complete;
- required automated/manual tests recorded;
- final traceability has no unexplained planned or in_progress rows;
- release artifacts map exactly to tagged commit.
