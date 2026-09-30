# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G9 — Skip Recommendations Discovery
- Active branch: `feat/discovery-skip-recommendations`
- Status: READY
- Task: `tasks/G9_DISCOVERY_SKIP_RECOMMENDATIONS.md`
- Accepted official baseline: `56aaba20b7d01746d616a2c8517adcf442950b90`
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
G4 implementation CODE-COMPLETE: all slices merged (PRs #21, #22, #24, #25, #26) with 0 new CI failures and the debug APK built. Its TCL C6K functional checks and same-file A/B remain MANUAL-PENDING and are intentionally batched with all other device-only checks for G14 final hardware certification. G4 therefore no longer blocks development.

G5 CODE-COMPLETE (PRs #31–#36, final `b973384`; 1903 tests, 15 known failures, 0 new, fullDebug APK).
Audit `docs/audits/G5_AUDIO_VIDEO_AUDIT.md`; D047 (batched validation), D048 (AUDIO_DV_AFR AUTO,
official output kept by default: every G5 change is a user-chosen path, a failure path, or a
per-format control defaulting to official). Slices: G5a soft clip (48, #32); G5b lossless default
(37, setting default off, #33); G5c per-format passthrough switches + HUD `chain`/output channels/
bitrate (38–42, #34); G5d DV failed-RPU drop, preserve-mapping fix (bundled libdovi maps native 4 to
static 8.4), EL type + RPU metadata HUD `dv` row, HDR10 SEI on the strip path (setting default off),
display mode size and `tv hdr` rows (52–54, 60, #35); G5e track-format AFR fallback with start hold +
2 s settle, < 20 fps floor (58, 59, #36). Verified official: 32–36, 43, 44, 47, 49–51, 55, 56, 57.
Deferred: 46 (TrueHD/DTS-HD cold start, device data needed, G14). Device checks HV-G5-1..HV-G5-10
are MANUAL-PENDING (G14; `validation_pending_gates`).

G6 CODE-COMPLETE (PRs #39, #41, #42, final `78fc577`; 1967 tests, 15 known failures, 0 new, fullDebug
APK). Pre-G6 upstream sync PR #40 (`52b3a71`, D050) adopted official `56aaba2` with official Subtitle
AutoSync (#3703): it is the single G6 engine (REUSE; no fork engine, D049 superseded). Audit
`docs/audits/G6_SUBTITLE_INTELLIGENCE_AUDIT.md`; D051 (SUBTITLE_INTELLIGENCE AUTO, only on AutoSync's
failure path or user-applied presentation). Slices: G6a stream-provided subtitle reference when the file
has no embedded one (84) + Arabic AutoSync strings (94) (#41); G6b custom subtitle fonts (Reshaped
FILE_PORT: validated .ttf/.otf, QR/LAN upload behind a per-session token, HTTPS-only URL import, Exo
typeface + libass `sub-font`, official-font fallback; 100–104) + Arabic cinema preset (105) (#42).
Verified official: 82, 83, 86–91, 95–99, 106. Deferred: 85 (no per-subtitle hash-match flag), 92, 93
(audio / ASR sync, G13 candidates). Device checks HV-G6-1..HV-G6-5 are MANUAL-PENDING (G14;
`validation_pending_gates`).

G7 CODE-COMPLETE (PRs #44, #45, #46, final `33a4f19`; 2011 tests, 15 known failures, 0 new, fullDebug
APK). Audit `docs/audits/G7_SEEK_INTELLIGENCE_AUDIT.md`; D052 (one preview engine, SEEK_INTELLIGENCE AUTO).
Slices: G7a hybrid seek preview engine in `fork/seek` (Reshaped FILE_PORT: thumbnails from the keyframes
playback already downloads, keyframe-exact commit, memory-bounded Seekr fallback with the user's
Keystore-encrypted key, Preview Sync, limits from `AdaptiveResources.seekPreviewBudget`; 107–110, 112, 115,
116, and G2's 279) (#45); G7b automatic Seekr calibration (Cxsmo estimator on local keyframe frames, no
live-player seeking; 111, 113, 114) (#46). Device checks HV-G7-1..HV-G7-5 are MANUAL-PENDING (G14;
`validation_pending_gates`).

G8 CODE-COMPLETE (PRs #48, #49, #50, #51, final `3c881e6`; 2047 tests, 15 known failures, 0 new, fullDebug
APK). Audit `docs/audits/G8_STREAM_INTELLIGENCE_AUDIT.md`; D053 (one ranking engine, STREAM_INTELLIGENCE AUTO).
Verified official: 148, 152–154, 157, 167. Slices: G8a progressive AIOStreams (Cxsmo FILE_PORT; snapshots
replace the add-on's group through official dedup; official request on any failure) and one bounded add-on
retry (G1's 289) (#49); G8b one ranker over official facts (Cxsmo chain + TRaSH tiers, HDR/DV by display,
lossless audio, add-on health), BEST_QUALITY autoplay mode (stream screen and next episode) and opt-in
"Sort streams by quality" list order (#50); G8c connection fit (Reshaped FILE_PORT: passive throughput per
network kind, heavy streams drop within their cache tier) (#51). Device checks HV-G8-1..HV-G8-7 are
MANUAL-PENDING (G14; `validation_pending_gates`).

G9 Skip / Recommendations / Discovery READY on `feat/discovery-skip-recommendations` (task
`tasks/G9_DISCOVERY_SKIP_RECOMMENDATIONS.md`, IDs 117–146, 169–187, 206–207). Official `dev` observed at
`7f32b3c` (Greek strings, Grid Home hero ratings, Exo libass fix; no G9 seam; not accepted).

## Exact next action
1. Start G9 with its mandatory audit (official skip-intro repository, post-play recommendations, episode
   shuffle vs Cxsmo skip aggregator, credentials store, calendar, random/mystery episode, dimmer per SOURCE_MAP),
   one owner per concern.
2. Continue G9→G13 without stopping for device-only MANUAL-PENDING checks. Record each one honestly in the gate docs/manual-test log and carry it into `validation_pending_gates`.
3. In G14, stop once for the consolidated hardware-certification campaign and execute all accumulated device/manual checks, including `docs/HARDWARE_VALIDATION_TCL_C6K.md`.
4. PR #28 is the governance review-queue PR; it is independent of runtime work.
5. Upstream: official `56aaba2` accepted (PR #40, D050). Before each gate, check official `dev`; sync through a reviewed `chore/upstream-sync-*` PR when the gate touches changed seams.

## Evidence / limits
Local Android execution is unavailable (no Android SDK); use GitHub CI evidence. Actions artifacts
cannot be downloaded here (blob storage egress blocked); the baseline-audit workflow prints the
inventory to the job log. No hardware/manual tests run or claimed. MANUAL-PENDING from G1: TV focus
of the add-on badge; on-device HUD values (refresh, passthrough, DV); D-pad focus/apply/revert on
the device assessment card. MANUAL-PENDING from G2: memory/scrolling (G2a) and startup/seek with the lower budget (G2b) on
1–2 GB boxes; source-panel focus smoothness and RTL rendering (G2c). MANUAL-PENDING from G3: D-pad focus on the
strategy card; each strategy's playback on real REMUX/HLS files (HUD `strategy` row). MANUAL-PENDING from G5:
HV-G5-1..HV-G5-10 (soft clip, lossless default, per-format passthrough, audio HUD rows, DV conversion
and strip-path SEI, DV stream info, display output, 23.976/24 matching, track-format AFR fallback).

## Ownership / unfinished work
Sequential writer, no lease. Admin actions (default branch, protection) remain in
GITHUB_ADMIN_CHECKLIST; connector access to those settings is not available.