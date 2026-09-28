# Fork Research Notes

This file is the implementation evidence map gathered before integration. It exists so future agents do not need the original chat history.

All source branches are additionally pinned in docs/SOURCE_MAP.md. Re-check source history only when intentionally refreshing a pin.

## Official — NuvioMedia/NuvioTV

Pinned integration baseline:
- branch: dev
- SHA: c257a2365ee3386b582dc2974ec235cfe0381f33
- verified 2026-09-28

Important current-official capabilities already found:
- ParallelRangeDataSource
- StreamSpeedTester
- libdovi / Dolby Vision-related code
- PlayerDebugStatsOverlay
- MDBList support
- custom server / discovery infrastructure
- modern PlayerRuntimeController split
- SkipIntroRepository with current official providers
- Nuvio Engine torrent path

Implication:
Never assume a feature mentioned by an older fork is still fork-only. Diff first.

## DavidVamaiotu/NuvioTV-Reshaped

Pinned:
- branch: subtitle-autosync
- SHA: 0ccf049d2789600835f3f7a75423e9149ea416ba

Primary harvest:
- Subtitle AutoSync
- audio-sync fallback
- secondary subtitle language
- custom subtitle fonts
- local seek previews / Seekr work
- disk seek buffer
- volume boost
- pill navigation
- connection-fit sorting
- Live TV

Previously audited implementation details / commits:
- 619c7e0eeb — seek-buffer default reset to Nuvio default while retaining choices
- 39370f90 — TV playback performance fixes
- 7b29022 — QR phone pages use Wi-Fi/Ethernet LAN address and avoid VPN addresses
- ccf6f750 — playback-session cleanup, embedded subtitle cue clearing, cheaper audio sync
- ed84221 — audio sync starts with one connection and expands conservatively; memory-aware
- 5ee3ff — bounds Seekr sprite memory; compressed storage; nearby sheets decoded
- c850c6 — AutoSync / preview hot-path optimization
- 3655068 — seek buffer avoids retrying dead/rate-limited links and overlapping connections
- 3dd4b56 — preview frames track real keyframe time so preview and seek align
- 447d8b9 — Live TV OK behavior shows Now/Next before controls
- 363da53 — Xtream live links, retries, live AFR behavior, MPEG-TS first-I-frame handling, logos
- 8ec4cb — reduces dense-dialogue false shifts in AutoSync
- e3e2bc — 100–200% volume boost with soft clipping and MPV scale handling
- 6ed4f38 — Live TV per-source jobs, categories, memory-conscious previews
- e3ff4a — current program / time-left / progress bar
- 8e1c9b — liquid-glass pill where capable, static fallback
- e7d56b — category reorder and hidden channels
- 943a2a — player channel-list categories
- 467af8e — multiple Live TV sources, category picker, muted preview, remote-safe search
- 51c975 — removes periodic preview/debug work causing judder
- 4ea064 — connection-speed sorting; preserves addon order except streams that exceed connection; sampled after active playback and remembered per network
- 0148e943 — seek-buffer robustness and low-RAM MPV cache caps
- e07409 — disk read-ahead ring file for ExoPlayer; no extra connection; 256MB/512MB/1GB choices; HLS/DASH/loopback excluded; MPV demux cache path
- ccb534 — optional top pill menu
- 1261a67 — local on-device seek previews from existing keyframes without extra network
- 72e1cb — custom TTF/OTF subtitle fonts with QR/URL/picker validation and fallback
- d0b703 — secondary subtitle language before audio fallback

Integration caveat:
Reshaped's disk seek-buffer philosophy and ysosrs parallel-range throughput path must not be blindly stacked. Treat them as distinct strategies coordinated by the playback strategy layer.

## ysosrs123/NuvioTV-Fork

Pinned:
- branch: nuvio-test
- SHA: 45e0984c18460d2a65c5d745999011b4314328eb

Primary harvest:
- high-bitrate 4K REMUX / lossless playback work
- device assessment
- detailed audio passthrough controls
- Dolby Vision handling
- frame-rate / HDMI-eARC behavior
- failover / malformed-container recovery
- advanced diagnostics / add-on health

Developer-reported claim:
The fork author reported large reductions in wasted bandwidth versus an older upstream parallel path on a measured REMUX. Treat this as developer-reported evidence, not an independent benchmark. Re-test against current official.

Previously audited behavior:
- Device Settings Assessment uses last-played link/hardware and suggests connection count, buffer and DV settings with apply/revert
- per-format passthrough toggles for DD, DD+, TrueHD, DTS, DTS-HD
- software decode fallback for unsupported sink formats
- TRaSH-like release quality ranking
- libdovi profile conversion work
- lossless audio selection
- precise 23.976/24 frame-rate handling
- eARC settle/resume handling
- MP4 seek fixes
- startup search/rank/pre-resolve/network warming
- poster prefetch/predecode
- detailed Stats for Nerds: measured video bitrate, audio codec/bitrate, actual passthrough, HDR/DV, network, buffer, audio route, SoC/throttle, jitter/underrun

Additional older behavior worth auditing:
- keep 429/503 non-fatal during rate-limit streaks
- replace hung reader/chunk with fresh connection
- malformed MKV cluster resync
- source failover during unrecoverable mid-play errors
- output resolution HUD
- lower direct-audio start threshold
- FFmpeg/Kodi-model downmix diagnostics

Do not inherit fork-specific removals:
- Modern-only layout policy
- removal of in-app updater
- removal of IAMF/MPEG-H merely because the fork removed them

Also note:
A significant amount of former ysosrs work has already converged into official Nuvio. Always delta-port.

## Cxsmo-ai/NuvioTV-Custom

Pinned:
- branch: main
- SHA: 3e0d0fad60a2721adec133b88640b49c0183883f

Primary harvest:
- Calendar
- Random Episode / Mystery Mode
- Seekr calibration
- App Dimmer
- post-play provider selection
- multi-provider skip metadata
- progressive AIOStreams
- scrape timeout modes
- D-pad/remote hardening
- UI/top-nav ideas

Detailed capabilities:
- Calendar combines watched/tracked shows, metadata-addon dates, local history/library and tracker sources
- Seekr calibration compares thumbnails to rendered frames at anchors and computes a bounded offset; rejects weak matches
- post-play providers: Auto, Trakt, TMDB, Simkl, MDBList, Kurato AI, BingeCat AI
- post-play catalogs can be discovered from installed addons, with pagination and lazy metadata
- skip providers include SkipMe.db, IntroDB, TheIntroDB, PublicMetaDB, MovieHavenDB, VideoSkip, NotScare
- skip categories include intro, recap, outro/credits, preview and content-warning style segments
- overlapping evidence can be merged with provider weighting
- conflicting actions such as skip vs mute should remain distinct
- progressive AIOStreams uses cumulative NDJSON snapshots where supported, with ordinary manifest fallback
- scrape timeout modes: instant, bounded, unlimited with safeguards
- duplicate-click / remote hardening

Important audited commits:
- f0fd4df93 — Random Episode, scope options, unwatched fallback, Mystery Mode
- 4cc476 — IntroDB movie segments / external skip controls
- 545c193 — FFmpeg downmix distortion/buffer-growth fix
- dd2b0e — truncated MKV tails treated as EOF
- ce868ca — nested MKV SeekHead
- 6db853 — avatar resolution and mute during Seekr calibration
- 681f34 — tracker credential dialog and TMDB episode-ratings fallback
- e490d13 — native full-app dimmer
- ef8e3e / 4c0358 — deterministic remote / duplicate Shield click handling
- d4e03e / 8514e0 / 312ea8 — calendar, artwork/spoiler and watched-state updates
- 6333cc / 1c5d / ee0c — top navigation / hero / Aero glass family
- 1c748b — detail trailer audio control
- 05393e / b1cdb7 / b0e405 — Kurato/BingeCat post-play work

Do not import as stable:
Smart Vibrance experiments were reverted. Treat as excluded unless reintroduced later as a fresh experimental project.

Progressive AIOStreams caveat:
Some progressive behavior may require a compatible endpoint/client mode. Capability-detect and retain normal fallback.

## hackerslash/NuvioTV-Lite

Pinned:
- branch: dev
- SHA: 2afdcd05d45e27afd48fb83ef9db6c286216a44c

Primary harvest:
adaptive resource logic, not a permanently stripped-down app flavor.

Audited ideas:
- physical-RAM tiering
- separate low-RAM comfort limits vs hard allocation-safety constraints
- larger devices keep full cache/fan-out
- bounded buffer/chunk ceilings on constrained devices
- optional expensive work reduced on weak devices
- Bidi/text-direction work kept out of hot recomposition paths
- stream-list recomposition optimizations
- safer subtitle-header forwarding to same host/subdomains
- bounded offline sync queue
- lower moov cache on low RAM
- RGB565 only when needed
- bounded caches
- low-RAM poster/image strategies

Integration decision:
Use these policies in AdaptiveResourceManager; do not produce a feature-deleted Lite fork as the main product.

## SPxMM3R1/NuvioTV-VibeSubtitle

Pinned:
- branch: vibe-dev
- SHA: 9520190184c7299c7ac34617856adde53c1ce7a2

Primary harvest:
language-independent timing alignment ideas.

Important commits:
- 4bb6b69d4 — aligns target-language addon subtitles to English timing via cue rhythm; uses cue order, durations, gaps, punctuation and timeline behavior; estimates offset, scale and piecewise drift; fails closed on low confidence
- 952019018 — embedded English subtitle reference path / EmbeddedSubtitleCueCollector work

Integration decision:
Generalize beyond Spanish. Fold algorithms into one Subtitle Intelligence engine, not a second independent subtitle-sync system.

## AntoninoScardina/NuvioTV — Watch Party

Pinned:
- branch: watchparty
- SHA: ff597b12bc7834b07dedda92c88e0d574c0aad81

Primary harvest:
Watch Party.

Audited implementation:
- VDO.Ninja SDK in hidden WebView / WebRTC data channel
- host shares current resolved stream URL + required headers
- play/pause/seek sync
- host state broadcast periodically and on actions
- small drift corrected with temporary speed change preserving pitch
- large drift corrected by seek
- TV <-> phone support
- six-character room code
- guests can use the exact resolved HTTP source without identical addons
- local torrent/P2P streams are not generally shareable as ordinary resolved HTTP links
- IP-locked links can fail between networks

Required hardening in our integration:
- explicit permission before sharing resolved URL/headers
- redact sensitive URL/header data from logs
- do not persist sensitive values
- clean room/session lifecycle
- retain encrypted transport behavior

Third-party notice:
Audit VDO.Ninja licensing/notices (MPL-related obligations) during import.

## Fornace/nuvio-ai

Pinned:
- branch: dev
- SHA: 518af7120c82d014c02f7717f4c9c6e396eb215c

Primary harvest:
optional AI Media Provider platform.

Audited architecture:
- AI Media Providers
- generated-dialogue subtitle provider concept
- translated voice overlay concept
- BYOK
- per-profile credentials encrypted with Android Keystore
- provider APKs from registry
- pinned SHA-256 verification
- exact signer-set verification
- host re-verifies identity/engine contract
- providers run as separate APKs rather than in-process plugins

Engine-adapter examples previously observed:
- openai-asr
- cloudflare-workers-ai
- qwen-livetranslate-ws
- openai-realtime-translate
- gemini-live-translate

Status:
Experimental/off by default. External services may be paid and/or preview quality.

Security:
Keep credentials out of loggable session URLs.

## xnucade/NuvioGlass

Pinned:
- branch: dev
- SHA: 84098b7de222fb599e30d12f7a6f5b9b1e347fee

Primary harvest:
Glass visual language / selectable layout.

Audited implementation:
- Glass HomeLayout
- frosted top navigation
- clock
- full-bleed hero
- calendar concept
- reusable glass components/surfaces/badges
- Haze blur with fallback on older Android
- glass HUD treatment

Integration decision:
Use richer Cxsmo calendar behavior where appropriate, but NuvioGlass visual ideas can become a selectable style. Do not remove official layouts.

## qamermax/Nuvio-Cinema

Research result:
Primarily crimson theme / Netflix-inspired sidebar evidence. Treat as optional UI inspiration rather than a core playback source unless a concrete unique implementation is later identified.

## jake6042/NuvioTV-Enhanced and similar Enhanced forks

Research result:
Older custom-server/discovery work is no longer a reason to port the subsystem because official current dev already contains custom server infrastructure.

Rule:
Do not re-port an obsolete parallel self-host stack. Diff any other specific feature before use.

## 9000000/NuvioTV-Fast and low-signal mirrors

Research result:
Version numbers on downstream forks do not establish freshness. Some forks were significantly behind official despite larger-looking tags.

Rule:
No source is selected because of tag/version number alone. Require a concrete unique feature and source-level evidence.

## Mirrors / downstream copies

Several forks mostly mirror Cxsmo/other sources. Do not import duplicate code from mirrors when the original feature source is known.

## Cross-platform forks

tvOS/Tizen/WebOS/Apple-focused ports are different platform work. Do not mix them into Android TV core unless a future task explicitly targets cross-platform behavior.

## Research methodology for implementation agents

For each feature:
1. Start from the pinned source SHA.
2. Locate the exact feature commits/files.
3. Compare against current official files.
4. Identify overlap already upstream.
5. Choose the smallest import unit.
6. Port tests with the behavior.
7. Record the result in docs/IMPORT_LEDGER.md.
8. Do not update a source pin silently.
