# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G12 — UI Styles
- Active branch: `feat/ui-styles`
- Status: READY
- Task: `tasks/G12_UI_STYLES.md`
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

G9 CODE-COMPLETE (audit #53; G9a #54; G9b #55; G9c #56; G9d #57; G9e #58; G9f #59; closeout
corrections #61). Final squash `7552ef8853d3aca69ffb82f07dc3057f56246d13`; #61 exact PR head
`058d99135ee8d0164db76981b82354c81b1433df`, CI run
https://github.com/alrithy/NuvioTV/actions/runs/36777344928: 2111 tests, 15 known failures,
0 new, 1 skipped, fullDebug APK. All 51 IDs (117–146, 169–187, 206–207)
are terminal: 40 implemented, 11 verified_official, none deferred. Closeout evidence and slices:
`docs/audits/G9_CLOSEOUT.md`; audit/architecture remain D054 and the existing G9 audit.
One official skip aggregator, opt-in providers/categories, post-play on official controller,
shuffle on official picker, bounded Calendar, per-profile App Dimmer (default 0%, up to 90%).
DISCOVERY_SKIP_RECOMMENDATIONS OFF restores official; ThemeDataStore remains unchanged.
HV-G9-1, HV-G9-2, HV-G9-3, HV-G9-4, HV-G9-5, HV-G9-6, HV-G9-7 are all MANUAL-PENDING
for G14 in the hardware checklist, manual log and validation_pending_gates. No device run claimed.

Closeout review corrections (#61, from the four findings on draft #60 and one on #61): raw dialogs and
popups draw the same dimmer layer; Mystery route context survives process recreation and manual /
failed-autoplay source lists show neutral cards; skip HTTP (and the Simkl lookups behind AniSkip /
Anime-Skip) is cancellation-aware inside the six-second provider deadline; encrypted credential
writes change a non-secret cache revision with profile identity, so corrected keys are retried.

G10 Live TV CODE-COMPLETE (`feat/live-tv`, `tasks/G10_LIVE_TV.md`, IDs 208–236; closeout
`docs/audits/G10_CLOSEOUT.md`). Audit
`docs/audits/G10_LIVE_TV_AUDIT.md`: official has live-playback plumbing (type `channel`, live latch,
live-window retry) but no Live TV feature, so no ID is verified official. D055: one `fork/livetv` owner
(Reshaped FILE_PORT at `0ccf049`, adapted), source records Keystore-encrypted per profile, hashed
favorites / hidden / recent keys, own OkHttp client without logging, host-only logs, Stalker auth
headers only to the portal host; channels on the official player as `channel`; live-only rules gated
on the Live TV playback registry; AFR stays with G5e's owner (live branch); preview budget from
AdaptiveResources; menu entry off until enabled. Official `dev` observed at `5c1d9b0` (landscape
posters, strings, hero focus; no Live TV seam; not accepted).
Audit merged as #62 (`43a1dfb`). G10a merged as #63 (`09b808a`; 2136 tests, 0 new): `fork/livetv`
sources and readers (M3U, Xtream, Stalker), per-source jobs and failure isolation, source list
Keystore-encrypted per profile, hashed channel keys, own HTTP client with host-only logs; LIVE_TV AUTO.
G10b merged as #64 (`93ddc6e`; 2143 tests, 0 new): Live TV screen, source and category dialogs,
favorites / hiding / order / names by channel key, per-profile menu switch in Layout sidebar settings
(off by default), channels in the official player as type `channel`; 208–212, 219–225 implemented.
G10c merged as #65 (`4bc3621`; exact head `f6fe186`, run 36791938657: 2169 tests, 15 known
failures, 0 new, 1 skipped; APK artifact 11133235363): XMLTV guide, now / progress / time left and
guide logos, with the eight review corrections (source-scoped guide identities, SHA-256 filenames /
cache v2, duplicate-id aliases, partial reads never cached as complete, zero collectors cancel guide
IO, Refresh reloads guides when every source fails, AdaptiveResources download budgets). 214–215 and
217–218 implemented; 216 in_progress until displayed in G10e. HV-G10-1..4 MANUAL-PENDING.
G10d merged as #66 (`4c97b1b`; exact head `f1a48f0`, run 36827186218: 2175 tests, 15 known
failures, 0 new, 1 skipped; APK artifact 11146585441): live-only playback rules and live-aware AFR
(233–236), gated on the in-memory Live TV playback registry so VOD is unchanged. HV-G10-5
MANUAL-PENDING.
G10e merged as #67 (`55c7b03`; exact head `dc0dfb3`, run 36828493852: 2177 tests, 15 known
failures, 0 new, 1 skipped; APK artifact 11146921479): Live TV inside the official player (216,
228–232): ▲▼ / CH+/CH- zapping inside the picked list with the ExoPlayer kept across Live TV zaps,
banner, ◀ channel list and categories, OK Now/Next card; inert unless the playing URL is registered
as Live TV. HV-G10-6 MANUAL-PENDING.
G10f merged as #68 (`f374ccf`; exact head `6a7fe12`, run 36829761290: 2181 tests, 15 known
failures, 0 new, 1 skipped; APK artifact 11147038290): channel preview in the Live TV list (226,
227) sized by AdaptiveResources `liveTvPreviewBudget` (720p on constrained boxes, off by default on
low-RAM boxes), per-profile Channel previews / Preview sound. HV-G10-7 MANUAL-PENDING.
G10g merged as #69 (`5aba471`; exact head `70035de`, run 36831015949: 2186 tests, 15 known
failures, 0 new, 1 skipped; APK artifact 11147825837): set up Live TV from a phone (213) — a LAN page
on a random 128-bit token path (QR code beside the add-source form) for an M3U link or file, Xtream
or Stalker, Origin check and size caps, nothing logged. HV-G10-8 MANUAL-PENDING.
All 29 G10 IDs are `implemented`; HV-G10-1..HV-G10-8 are carried to G14 (`validation_pending_gates`,
MANUAL_TEST_LOG). No Android/device run is claimed.

G11 Watch Party CODE-COMPLETE (`feat/watch-party`, `tasks/G11_WATCH_PARTY.md`, IDs 237–249; closeout
`docs/audits/G11_CLOSEOUT.md`). Audit merged as #71 (`2d39a6d`; 2186 tests, 0 new) `docs/audits/G11_WATCH_PARTY_AUDIT.md`: official
(`56aaba2`, `dev` `5c1d9b0`) has no Watch Party but every player control a sync engine needs; source
AntoninoScardina/NuvioTV `watchparty` @ `ff597b1` (one commit, `5853027`; VDO.Ninja SDK v1.6.1,
MPL-2.0). D056: one `fork/watchparty` owner; wire format kept for the Nuvio Party phone build; host
consent per room before anything is shared; header allowlist, credential / torrent / local / Live TV
streams not shareable; guest media memory-only and never saved for reuse; no WebView console logs;
SecureRandom codes; WATCH_PARTY AUTO with G11a. Slices G11a (core, 237–242, 244–249) and G11b (UI,
consent, join, 243 and the UI of 237–240).
G11a merged as #72 (`74ee62f`; exact head `c507bc8`, run 36835101903: 2197 tests, 15 known
failures, 0 new, 1 skipped; APK artifact 11148704473): `fork/watchparty` core — protocol v1 (wire
format kept), host-authority sync with soft speed correction and hard seek, consent-gated rooms,
WatchPartySharePolicy, hidden WebView transport with the upstream VDO.Ninja SDK v1.6.1 (MPL-2.0,
source form), no console logs; WATCH_PARTY AUTO.
G11b merged as #73 (`1002530`; exact head `c4e3a95`, run 36836494526: 2198 tests, 15 known
failures, 0 new, 1 skipped; APK artifact 11149093802): Watch Party button in the player controls,
two-step consent before a room is created, room panel and badge, joining from Playback settings,
guests' players opening the host's stream; unshareable reasons (torrent / local, Live TV,
credentials); a guest's received link is kept in memory only and never saved for reuse
(`streamCacheKey` guard); strings en + ar. 237–249 implemented; HV-G11-1..HV-G11-4 MANUAL-PENDING.
Review corrections merged as #74 (`65e1c57`; exact head `af7de62`, run 36842866031: 2205 tests,
15 known failures, 0 new, 1 skipped; APK artifact 11152162286; D057) for the 14 Codex findings that arrived on #71, #72 and #73
after merge: the guest's player route carries a one-time in-memory ticket instead of the link and
headers (none in navigation saved state) and diagnostics never save a received link; non-public
destinations (loopback, private, CGNAT, link-local, ULA, multicast, reserved, LAN-only names,
non-dotted numeric hosts) are never shared or opened, and a guest checks resolved addresses before
opening; a guest follows one host until it leaves, and the host leaving clears its state; host speed
travels as an optional `rate` (omitted at 1×) and guests correct around it and get their own speed
back; terminal signaling loss ends the room; remote play / pause does the button's mpv bookkeeping;
unshareable streams are detached; streams without a duration attach; guests see an unsupported-stream
notice (IP-locked / local sources). Phone compatibility target pinned: AntoninoScardina/NuvioMobile
`watchparty` @ `ff7a16b` (reference only). All 14 review threads on #71–#73 were answered and resolved.
All 13 G11 IDs are `implemented`; HV-G11-1..HV-G11-4 are carried to G14 (`validation_pending_gates`,
MANUAL_TEST_LOG). No Android/device run is claimed.
G12 UI Styles READY on `feat/ui-styles` (`tasks/G12_UI_STYLES.md`, IDs 188–205 and 292), owner
Claude (sequential; no lease), after this closeout merges; next G13 Experimental AI & MAT.

## Exact next action
1. Start G12 UI Styles with an official-first audit (`docs/audits/G12_UI_STYLES_AUDIT.md`) against
   official `56aaba2` / `dev` and the pinned NuvioGlass, Reshaped and Cxsmo sources.
2. Preserve navigation/player bridges, official VOD behavior, one owner per concern and resource bounds.
3. Continue G12→G13 after exact-head automated DoD; device-only checks remain MANUAL-PENDING for G14.
4. In G14 execute all accumulated hardware checks, including `docs/HARDWARE_VALIDATION_TCL_C6K.md`.
5. PR #28 remains independent governance work. Official pins change only through a dedicated sync PR.

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