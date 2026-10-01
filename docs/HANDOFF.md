# Nuvio Superfork handoff

<!-- canonical-state:start -->
- Active gate: G14 — Hardening Release Upstream
- Active branch: `chore/release-hardening`
- Status: BLOCKED
- Task: `tasks/G14_HARDENING_RELEASE.md`
- Accepted official baseline: `aeb6ee8591c55424256fdd1f35f426618297378d`
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
G11 closeout merged as #75 (`aa0ec42`; exact head `d79e2f2`, run 36844691833: 2205 tests, 15 known
failures, 0 new, 1 skipped; APK artifact 11153062880).

G12 UI Styles CODE-COMPLETE (`feat/ui-styles`, `tasks/G12_UI_STYLES.md`, IDs 188–205 and 292;
closeout `docs/audits/G12_CLOSEOUT.md`). Audit merged as #76 (`bd1f13e`; exact head `86a521f`, run 36846143142: 2205 tests, 15 known failures,
0 new, 1 skipped; APK artifact 11153369840) `docs/audits/G12_UI_STYLES_AUDIT.md`: official
`56aaba2` already has Classic / Grid / Modern per profile, both sidebars (modern with icon pill and
Android 12+ blur), the rotating hero carousel, the Modern hero trailer and full-screen backdrop
(verified_official 188–192, 195, 201–203); it has no top navigation, Glass layout, clock,
screensaver or liquid glass. D058: one `fork/uistyle` owner (NuvioGlass `84098b7` surface / nav pill /
clock / scaffold, Reshaped `0ccf049` pill and AGSL liquid glass, Cxsmo `3e0d0fa` top-bar profile
access and screensaver); one top-chrome scaffold with BAR / PILL / GLASS looks; per-profile
navigation style (Sidebar default); `HomeLayout.GLASS` and `CINEMATIC_GLASS` on the Modern pipeline;
effects gated by AdaptiveResources tier and API level with a flat fallback; screensaver off by
default; UI_STYLES AUTO with the first code slice. Official `dev` `5c1d9b0` changed the home layout
files, so an upstream sync PR precedes G12 code: #77 (`chore/upstream-sync-2026-10-01`) merges
official `dev` `5c1d9b0` with no conflicts (integration merge `89b46c4`); clean replays (run 36848159277):
official 1,796 tests identical to the `56aaba2` inventory, integration 2,205 tests, 15 known, 0 new;
baseline re-anchored on `5c1d9b0` (D059) with debt unchanged.
Upstream sync merged as #77 (`c45b78e`, merge commit; exact head `dbdca31`, run 36849295963: 2205
tests, 15 known failures, 0 new, 1 skipped; APK artifact 11155351900).
G12a merged as #78 (`14a3353`; exact head `362dd45`, run 36855538291: 2209 tests, 15 known failures,
0 new, 1 skipped; APK artifact 11159113616): `fork/uistyle` top menu. Per-profile Navigation style (Sidebar default, Top bar,
Pill) replaces official's sidebar with one `TopChromeScaffold` in two looks (NuvioGlass sliding-
indicator menu and minute-tick clock, Cxsmo top-bar profile access, Reshaped pill look); the menu sits
above the content (Up from the top row reaches it; Back focuses it, then exits; long-press Back jumps
to it); official `navigateToDrawerRoute` / `rememberRawSvgPainter` made internal and reused; clock in
the device 12 / 24-hour format; profile button opens profile selection; UI_STYLES AUTO. 196, 198,
204, 205 implemented; HV-G12-1, HV-G12-2 MANUAL-PENDING. Slice plan adjusted: the effect policy, liquid
glass (199) and lightweight fallback (200) land with the overlaid Glass chrome in G12b, where they have
a backdrop to work on. Local JVM harness: 334 tests PASS (UI compiles only in CI).
G12b merged as #79 (`c98dc01`; exact head `ec93e48`, run 36858515752: 2212 tests, 15 known failures,
0 new, 1 skipped; APK artifact 11160717187; D060 amends D058): Glass is a fourth Navigation style rather than new `HomeLayout`
values: official's Modern home with frosted chrome floating over the full-bleed hero (NuvioGlass
`GlassScaffold` / `GlassSurface` FILE_PORT), auto-hiding on Home and revealed by Up from the first row
(one hook in `ModernHomeRowsList`); Pill with Classic / Grid; live Haze blur only on Android 12+ with
a known non-LOW_RAM tier and the per-profile Lightweight effects switch off, otherwise an opaque tint;
Back / long-press Back as the other top menus; never over the player. 193, 197, 200 implemented;
HV-G12-3 MANUAL-PENDING. Local JVM harness: 337 tests PASS.
G12c merged as #80 (`783973f`; exact head `c81156f`, run 36863863735: 2213 tests, 15 known failures,
0 new, 1 skipped; APK artifact 11162099759; D061): Cinematic Glass is a fifth Navigation style, Glass with official's Modern
full-screen hero backdrop shown through `LocalCinematicGlass` (one line in `ModernHomeContent`; the
official preference is never written; the hero trailer keeps following official's settings); Reshaped's
AGSL liquid-glass lens (shader + backdrop recorder FILE_PORT) under the Glass pills as a third effect
level, LIQUID only on Android 13+ in the STANDARD tier with Lightweight effects off; the recorder runs
only while the chrome shows. 194, 199 implemented; HV-G12-4 MANUAL-PENDING. Local JVM harness: 338
tests PASS.
G12d merged as #81 (`4d640fc`; exact head `b4c92e4`, run 36865550286: 2219 tests, 15 known failures,
0 new, 1 skipped; APK artifact 11163438418; D062): the optional screensaver, Cxsmo's controller as a pure `ScreensaverMachine`
plus overlay; per profile, off by default, under Appearance (start after 1–30 min, darkness
50 / 70 / 85 %); never while playing or buffering, waits while a dialog has focus, the waking press is
swallowed, hero trailers do not start under it. One-line hooks in MainActivity (1 Hz check, key
dispatch, window focus, overlay), PlayerViewModel (playing state) and TrailerPlayerPool (acquire).
292 implemented; HV-G12-5 MANUAL-PENDING. Local JVM harness: 344 tests PASS.
All 19 G12 IDs are terminal (10 implemented, 9 verified_official); HV-G12-1..HV-G12-5 are carried to
G14 (`validation_pending_gates`, MANUAL_TEST_LOG). No Android/device run is claimed.
G12 closeout merged as #82 (`2791c1d`; exact head `0729e9e`, run 36874872121: 2219 tests, 15 known
failures, 0 new, 1 skipped; APK artifact 11169431821). Legacy PR #1 / #2 stay reference-only.

G13 Experimental AI & MAT CODE-COMPLETE (`experimental/media`, `tasks/G13_EXPERIMENTAL.md`, IDs 45 and
250–261; closeout `docs/audits/G13_CLOSEOUT.md`). Audit merged as #83 (`a62ce1d`; exact head `498da52`, run 36877526195: 2219 tests, 15 known failures,
0 new, 1 skipped; APK artifact 11170525995) `docs/audits/G13_EXPERIMENTAL_AUDIT.md`:
official `5c1d9b0` has no AI and only platform passthrough; official `dev` `9e17941` (seven commits:
MDBList cache, stream focus, strings) touches no G13 seam, so no sync PR. Fornace/nuvio-ai `518af71`
ships the provider platform (registry, SHA-256 and exact-signer-set checked provider APKs, Messenger
negotiation, AAD-bound per-profile BYOK vault, dedicated platform-TLS client, Provider Center) but
its providers are contract shells, its subtitle path is a fake and its voice overlay is unwired.
D063: one `fork/aimedia` owner for the platform (250, 255–261) and the ysosrs `45e0984` MAT path in
`fork/audio` (45); 251–254 deferred until a pinned source ships an engine; AI traffic never uses
official's clients (the playback client falls back to trust-all); a Settings → Experimental screen
with OFF-by-default switches is the only way G13 turns on. Slices: G13a (pure core + opt-in), G13b
(Android bridges, Provider Center, Experimental screen), G13c (MAT). Next G14.
G13a merged as #84 (`b5da995`; exact head `fdc8640`, run 36879784424: 2290 tests, 15 known failures,
0 new, 1 skipped; APK artifact 11170983846): `fork/aimedia` core, FILE_PORT of Fornace `518af71` (registry and vendor-catalog
models, constant-time SHA-256 artifact verifier, exact-signer-set contract validator, AAD-bound
per-profile credential vault behind a Keystore bridge seam; Fornace tests ported) and
`ExperimentalOptIn` (device-wide store read in `NuvioApplication.onCreate` before any registry read;
only experimental groups can be opted in; the registry's default overrides are the opt-ins).
Nothing is constructed at runtime yet. 255, 256, 259–261 in_progress (status `experimental` once
the G13b flow lands). Local JVM harness: 415 tests PASS.
G13b merged as #85 (`a4f66f5`; exact head `ebe2e49`, run 36882099635: 2323 tests, 15 known failures,
0 new, 1 skipped; APK artifact 11173800583): the Provider Center and its Android host (Fornace FILE_PORT): registry client and
APK downloader on the dedicated platform-TLS client, PackageInstaller bridge, package scanner,
Messenger contract client, install coordinator, controller and screen, vendor selection, Keystore
and SharedPreferences storage; Settings → Advanced → Experimental holds the AI opt-in and opens the
Provider Center (full-screen dialog) while it is active, with a preview notice (providers are
contract shells); profile deletion deletes AI keys through official's `ProfileScopedCredentialStore`
set (vault resolved lazily); the manifest defines the provider bind permission and package queries.
250, 255–261 `experimental`; HV-G13-1, HV-G13-2 MANUAL-PENDING. Local JVM harness: 448 tests PASS.
G13c merged as #86 (`746c82a`; exact head `a8eeb15`, run 36885503149: 2330 tests, 15 known failures,
0 new, 1 skipped; APK artifact 11174408183): MAT (45), ysosrs `45e0984` FILE_PORT into `fork/audio/mat` (TrueHD framer, Kodi-
derived MAT packer, IEC 61937 sink, routing sink); the wrap happens only while MAT passthrough is
opted in (Settings → Advanced → Experimental), otherwise official's audio path is untouched; the
speed-aware renderer keeps the G5 passthrough policy and Bluetooth PCM while writing into the
wrapper; MatFramingTest (the source has none). 45 `experimental`; HV-G13-3 MANUAL-PENDING. Local JVM
harness: 455 tests PASS.
G13 has 9 IDs `experimental` (45, 250, 255–261; OFF unless opted in) and 4 deferred (251–254, no
engine in any pinned source; re-audit when Fornace ships engine providers); HV-G13-1..HV-G13-3 are
carried to G14 (`validation_pending_gates`, MANUAL_TEST_LOG). No Android/device run is claimed.
G14 Hardening / Distribution / Upstream READY on `chore/release-hardening`
(`tasks/G14_HARDENING_RELEASE.md`, IDs 293–310, 312, 316, 320), owner Claude (sequential; no lease),
after this closeout merges. It is the last gate. Legacy PR #1 / #2 stay reference-only.
G13 closeout merged as #87 (`4a7115a`; exact head `7980995`, run 36887451146: 2330 tests, 15 known
failures, 0 new, 1 skipped; APK artifact 11175173134).

G14 Hardening / Distribution / Upstream IN_PROGRESS on `chore/release-hardening`, task
`tasks/G14_HARDENING_RELEASE.md`, owner Claude (sequential; no lease). Audit merged as #88 (`45a798a`; exact head `fc7b6be`, run 36889863343: 2330 tests, 15 known
failures, 0 new, 1 skipped; APK artifact 11176657814; D064, D065)
`docs/audits/G14_HARDENING_AUDIT.md`: official `5c1d9b0` already ships self-hosted server discovery,
switching, trust confirmation and return to official (293–298), profile-scoped credential cleanup
(300), on-device AutoSync (301), subtitle header scoping (302) and ABI splits (307), all
verified_official. Gaps: official's five local QR configuration servers have no session token or
Origin check and expose add-on URLs; `urlForLog` is an identity and several official sites log raw
add-on / stream URLs; the `full` updater points at official's releases and never checks a digest;
the release workflow runs only the updater tests and publishes no checksums; nothing watches official
`dev`. Official `dev` `aeb6ee8` (eleven commits, player error path) merges cleanly; a sync PR precedes
G14 code (D065): #89 (`chore/upstream-sync-2026-10-01-g14`, merge commit `8832fa0`; exact head `9ca1b50`, run 36893473195:
2,358 tests, 15 known failures, 0 new, 1 skipped; APK artifact 11179166278) merges it with no conflicts
(integration merge `ed62c7b`); clean replays (run 36891605846): official `aeb6ee8` 1,824 tests (the 1,796
of `5c1d9b0` identical, 28 new passing), integration 2,358 tests, 15 known, 0 new; baseline re-anchored
on `aeb6ee8` with debt unchanged and the minimum test count raised to 1,824 (D065). Slices: sync, G14a (299, 303–305 `fork/security`), G14b (306, 308–310, 312
`fork/distribution`), G14c (316 upstream watch), then the 320-row accounting and the hardware
campaign (72 HV cases plus the G4 A/B), which only the maintainer can run.
G14a merged as #90 (`b33d343`; exact head `6c325b2`, run 36914173600: 2368 tests, 15 known failures,
0 new, 1 skipped; APK artifact 11188249495): `fork/security` `LocalServerAccess` gates official's five QR configuration servers
(the QR code's per-session key is exchanged for an HttpOnly SameSite=Lax session cookie; no session →
403; writes from another site's page refused by Origin / Referer; one-line hooks, official pages
untouched; the fork font and Live TV servers share the rule) and `urlForLog()` keeps scheme, host and
port only, with the add-on, stream, subtitle and plugin URL log sites routed through it. 299, 303,
304, 305 implemented; HV-G14-1, HV-G14-2 MANUAL-PENDING. Local JVM harness: 465 tests PASS.
G14b merged as #91 (`bf939fb`; exact head `78235f0`, run 36916054114: 2373 tests, 15 known failures,
0 new, 1 skipped; APK artifact 11190935543): `fork/distribution`: the `full` updater reads the fork's own releases
(`FORK_UPDATE_OWNER` / `FORK_UPDATE_REPO`, default this repository) and installs an APK only when its
streamed SHA-256 equals GitHub's published asset digest (missing or different → deleted, refused);
official's stable / beta channels select among the fork's releases; the release workflow runs the
governance suite, validator and full unit suite with the baseline guard before building and
publishes `SHA256SUMS.txt`. 306, 308, 309, 310, 312 implemented; HV-G14-3 MANUAL-PENDING after the
first fork release (maintainer: signing secrets, first release and the application-id choice,
GITHUB_ADMIN_CHECKLIST D). Local JVM harness: 467 tests PASS.
G14c merged as #92 (`dc676bf`; exact head `d5832b8`, run 36917962782: 2373 tests, 15 known failures,
0 new, 1 skipped; APK artifact 11190309840): `scripts/superfork/upstream_watch.py` + Superfork Upstream Watch: official `dev`
commits after the accepted baseline and the fork seams they touch, as a job summary and one
annotation, on every PR, on demand and daily once `superfork/integration` is the default branch;
read-only, no secrets, no merges; test_upstream_watch. 316 implemented.
Accounting merged as #93 (`15a98c9`; exact head `87eefeb`, run 36920589024: 2373 tests, 15 known
failures, 0 new, 1 skipped; APK artifact 11192301846) `docs/audits/G14_ACCOUNTING.md`: 318 of 320 rows terminal with evidence, 3 (Auto network
input: needs the A/B thresholds; the G8c estimator is ready) and 320 (Stable distribution) blocked on
the maintainer's devices and release actions; ledger, license, dependency (Seekr recorded) and
sensitive-log audits done; the hardware campaign order. G14 BLOCKED: nothing in code remains.
Device test build (#94, open; D066): the maintainer's first device run had no TV QR login (the CI APK
has an empty backend configuration and a per-run signing key). `Superfork Test Build` builds
`com.nuvio.tv.debug` with the real backend configuration from secrets, one fixed test key and version
code `versionCode * 100000 + run`, verifies package / signature / configuration presence, and
publishes the universal APK; secrets go to runner files only. Needs the secrets in
GITHUB_ADMIN_CHECKLIST E.
## Exact next action
1. Wait for #94 (test build) exact-head CI and squash it; once the maintainer adds the secrets in
   GITHUB_ADMIN_CHECKLIST E, check the first Superfork Test Build run (package, signature, backend
   configuration present) and hand over its artifact. Then G14 waits on the maintainer: the
   hardware campaign (`docs/audits/G14_ACCOUNTING.md` order) and GITHUB_ADMIN_CHECKLIST A–D. Record
   shared evidence as PASS / FAIL, fix any FAIL, then close 3, 320 and G14. Official upstream movement
   is reported by Superfork Upstream Watch; sync through a dedicated PR when it touches fork seams.
2. Preserve navigation/player bridges, official VOD behavior, one owner per concern and resource bounds.
3. Hardware results are recorded only as real PASS / FAIL; a FAIL reopens the affected gate / feature.
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